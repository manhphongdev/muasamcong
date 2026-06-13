import http from 'node:http';
import { pipeline } from 'node:stream/promises';

const PORT = Number.parseInt(process.env.PORT || '8080', 10);
const API_KEY = process.env.API_KEY || '';
const AGENT_URL = process.env.AGENT_URL || 'http://127.0.0.1:1234/api/download/file/browser/public';
const DOWNLOAD_TIMEOUT_MS = Number.parseInt(process.env.DOWNLOAD_TIMEOUT_MS || '120000', 10);
const MODE = 'STREAM_ONLY';

const INVALID_FILE_NAME_CHARS = /[<>:"/\\|?*\x00-\x1F]/g;
const TEXT_RESPONSE_TYPES = ['text/html', 'application/json'];

const server = http.createServer(async (req, res) => {
  const startedAt = Date.now();

  try {
    const requestUrl = new URL(req.url || '/', `http://${req.headers.host || 'localhost'}`);

    if (req.method === 'GET' && requestUrl.pathname === '/health') {
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
  const apiKey = req.headers['x-api-key'];
  if (!API_KEY || apiKey !== API_KEY) {
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
      sendJson(res, 502, {
        error: 'AGENT_HTTP_ERROR',
        message: `Agent returned HTTP ${agentResponse.status}`,
      });
      return;
    }

    if (isTextLikeResponse(contentType)) {
      sendJson(res, 502, {
        error: 'AGENT_INVALID_CONTENT_TYPE',
        message: `Agent returned invalid content type: ${contentType}`,
      });
      return;
    }

    res.statusCode = 200;
    res.setHeader('Content-Type', contentType);
    res.setHeader('Content-Disposition', `attachment; filename="${escapeHeaderValue(fileName)}"`);

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

function escapeHeaderValue(value) {
  return value.replace(/["\\]/g, '_');
}
