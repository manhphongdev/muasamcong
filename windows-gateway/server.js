import http from 'node:http';
import { pipeline } from 'node:stream/promises';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// Load .env configuration file
function loadEnv() {
  try {
    let envDir;
    if (process.pkg) {
      envDir = path.dirname(process.execPath);
    } else {
      const __dirname = path.dirname(fileURLToPath(import.meta.url));
      envDir = __dirname;
    }
    const envPath = path.join(envDir, '.env');
    if (fs.existsSync(envPath)) {
      const content = fs.readFileSync(envPath, 'utf8');
      content.split(/\r?\n/).forEach(line => {
        const trimmed = line.trim();
        if (!trimmed || trimmed.startsWith('#')) return;
        const match = trimmed.match(/^\s*([\w.-]+)\s*=\s*(.*)\s*$/);
        if (match) {
          const key = match[1];
          let value = match[2].trim();
          if (value.startsWith('"') && value.endsWith('"')) value = value.slice(1, -1);
          if (value.startsWith("'") && value.endsWith("'")) value = value.slice(1, -1);
          if (process.env[key] === undefined) {
            process.env[key] = value;
          }
        }
      });
      console.log(`Loaded environment from: ${envPath}`);
    }
  } catch (e) {
    console.error('Failed to load .env file:', e.message);
  }
}
loadEnv();

const PORT = Number.parseInt(process.env.PORT || '18080', 10);
const API_KEY = process.env.API_KEY || 'secret';
const AGENT_URL = process.env.AGENT_URL || 'http://127.0.0.1:1234/api/download/file/browser/public';
const DOWNLOAD_TIMEOUT_MS = Number.parseInt(process.env.DOWNLOAD_TIMEOUT_MS || '120000', 10);
const MODE = 'STREAM_ONLY';
const MAX_ERROR_BODY_LENGTH = 1000;

const INVALID_FILE_NAME_CHARS = /[<>:"/\\|?*\x00-\x1F]/g;
const TEXT_RESPONSE_TYPES = ['text/html', 'application/json'];

const server = http.createServer(async (req, res) => {
  const startedAt = Date.now();

  try {
    const requestUrl = new URL(req.url || '/', `http://${req.headers.host || 'localhost'}`);

    if (req.method === 'GET' && requestUrl.pathname === '/health') {
      if (!isAuthorized(req)) {
        sendJson(res, 401, { error: 'UNAUTHORIZED', message: 'Invalid API key' });
        return;
      }
      sendJson(res, 200, {
        status: 'OK',
        agentUrl: AGENT_URL,
        mode: MODE,
      });
      return;
    }

    if (req.method === 'GET' && requestUrl.pathname === '/download') {
      await handleDownload(req, res, requestUrl, startedAt);
      return;
    }

    sendJson(res, 404, { error: 'NOT_FOUND', message: 'Route not found' });
  } catch (error) {
    if (!res.headersSent) {
      sendJson(res, 500, { error: 'INTERNAL_ERROR', message: safeMessage(error) });
    } else {
      res.destroy(error);
    }
  }
});

server.listen(PORT, () => {
  console.log(`Windows Gateway listening on port ${PORT}, mode=${MODE}, agentUrl=${AGENT_URL}`);
});

async function handleDownload(req, res, requestUrl, startedAt) {
  if (!isAuthorized(req)) {
    sendJson(res, 401, { error: 'UNAUTHORIZED', message: 'Invalid API key' });
    return;
  }

  const fileId = normalizeRequired(requestUrl.searchParams.get('fileId'));
  if (!fileId) {
    sendJson(res, 400, { error: 'INVALID_FILE_ID', message: 'fileId is required' });
    return;
  }

  const fileName = sanitizeFileName(requestUrl.searchParams.get('fileName'));
  const agentUrl = buildAgentUrl(fileId);
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), DOWNLOAD_TIMEOUT_MS);
  let status = 502;

  try {
    const agentResponse = await fetch(agentUrl, {
      method: 'GET',
      headers: {
        Accept: 'application/octet-stream, application/pdf, */*',
        Origin: 'https://muasamcong.mpi.gov.vn',
        Referer: 'https://muasamcong.mpi.gov.vn/web/guest/contractor-selection',
        'User-Agent': 'Mozilla/5.0',
      },
      signal: controller.signal,
    });

    status = agentResponse.status;
    const contentType = agentResponse.headers.get('content-type') || 'application/octet-stream';
    if (!agentResponse.ok) {
      const agentBody = await readSmallText(agentResponse);
      sendJson(res, 502, {
        error: 'AGENT_HTTP_ERROR',
        message: `Agent returned HTTP ${agentResponse.status}`,
        agentStatus: agentResponse.status,
        agentBody,
      });
      return;
    }

    if (isTextLikeResponse(contentType)) {
      const agentBody = await readSmallText(agentResponse);
      sendJson(res, 502, {
        error: 'AGENT_INVALID_CONTENT_TYPE',
        message: `Agent returned invalid content type: ${contentType}`,
        agentStatus: agentResponse.status,
        agentBody,
      });
      return;
    }

    res.statusCode = 200;
    res.setHeader('Content-Type', contentType);
    res.setHeader('Content-Disposition', contentDisposition(fileName));

    const contentLength = agentResponse.headers.get('content-length');
    if (contentLength) {
      res.setHeader('Content-Length', contentLength);
    }

    if (!agentResponse.body) {
      sendJson(res, 502, { error: 'AGENT_EMPTY_BODY', message: 'Agent response body is empty' });
      return;
    }

    await pipeline(agentResponse.body, res);
  } catch (error) {
    if (error?.name === 'AbortError') {
      status = 504;
      if (!res.headersSent) {
        sendJson(res, 504, { error: 'AGENT_TIMEOUT', message: 'Agent request timed out' });
      } else {
        res.destroy(error);
      }
      return;
    }

    status = 502;
    if (!res.headersSent) {
      sendJson(res, 502, { error: 'AGENT_REQUEST_FAILED', message: safeMessage(error) });
    } else {
      res.destroy(error);
    }
  } finally {
    clearTimeout(timeout);
    const durationMs = Date.now() - startedAt;
    console.log(`fileId=${fileId} status=${status} durationMs=${durationMs}`);
  }
}

function buildAgentUrl(fileId) {
  const agentUrl = new URL(AGENT_URL);
  agentUrl.searchParams.set('fileId', fileId);
  agentUrl.searchParams.set('downloadFilePublic', 'true');
  return agentUrl;
}

function isAuthorized(req) {
  return req.headers['x-api-key'] === API_KEY;
}

function normalizeRequired(value) {
  if (value == null) {
    return null;
  }

  const normalized = value.trim();
  return normalized.length === 0 ? null : normalized;
}

function sanitizeFileName(value) {
  const fallback = 'download.bin';
  if (value == null) {
    return fallback;
  }

  const sanitized = value
    .replace(INVALID_FILE_NAME_CHARS, '_')
    .replace(/\s+/g, ' ')
    .trim();

  return sanitized.length === 0 ? fallback : sanitized;
}

function isTextLikeResponse(contentType) {
  const normalized = contentType.toLowerCase().split(';')[0].trim();
  return TEXT_RESPONSE_TYPES.includes(normalized);
}

function sendJson(res, statusCode, body) {
  const payload = JSON.stringify(body);
  res.statusCode = statusCode;
  res.setHeader('Content-Type', 'application/json; charset=utf-8');
  res.setHeader('Content-Length', Buffer.byteLength(payload));
  res.end(payload);
}

function safeMessage(error) {
  if (!error) {
    return 'Unknown error';
  }
  return error.message || String(error);
}

async function readSmallText(response) {
  try {
    const text = await response.text();
    return truncate(text);
  } catch (error) {
    return `Cannot read agent response body: ${safeMessage(error)}`;
  }
}

function truncate(value) {
  if (!value || value.length <= MAX_ERROR_BODY_LENGTH) {
    return value;
  }
  return `${value.slice(0, MAX_ERROR_BODY_LENGTH)}...`;
}

function contentDisposition(fileName) {
  const fallback = asciiFileName(fileName);
  return `attachment; filename="${escapeHeaderValue(fallback)}"; filename*=UTF-8''${encodeRFC5987Value(fileName)}`;
}

function asciiFileName(value) {
  const normalized = value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^\x20-\x7E]/g, '_')
    .replace(/\s+/g, ' ')
    .trim();

  return normalized.length === 0 ? 'download.bin' : normalized;
}

function escapeHeaderValue(value) {
  return value.replace(/[^\x20-\x7E]/g, '_').replace(/["\\]/g, '_');
}

function encodeRFC5987Value(value) {
  return encodeURIComponent(value).replace(/['()*]/g, (char) => `%${char.charCodeAt(0).toString(16).toUpperCase()}`);
}
