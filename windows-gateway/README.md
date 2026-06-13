# Windows Gateway

Gateway Node.js chạy trên máy Windows để stream file từ Windows agent về backend.

Gateway không lưu file local. Backend là bên lưu file.

## Yêu Cầu

- Node.js 20 LTS trở lên.
- Windows agent đang chạy local:

```text
http://127.0.0.1:1234/api/download/file/browser/public
```

## Config

Biến môi trường:

```text
PORT=8080
API_KEY=<secret>
AGENT_URL=http://127.0.0.1:1234/api/download/file/browser/public
DOWNLOAD_TIMEOUT_MS=120000
```

## Chạy Gateway

Windows CMD:

```bat
set PORT=8080
set API_KEY=secret
set AGENT_URL=http://127.0.0.1:1234/api/download/file/browser/public
set DOWNLOAD_TIMEOUT_MS=120000
npm start
```

PowerShell:

```powershell
$env:PORT="8080"
$env:API_KEY="secret"
$env:AGENT_URL="http://127.0.0.1:1234/api/download/file/browser/public"
$env:DOWNLOAD_TIMEOUT_MS="120000"
npm start
```

## API

### GET /health

```bash
curl http://127.0.0.1:8080/health
```

Response:

```json
{
  "status": "OK",
  "agentUrl": "http://127.0.0.1:1234/api/download/file/browser/public",
  "mode": "STREAM_ONLY"
}
```

### GET /download

```bash
curl -L \
  -H "X-Api-Key: secret" \
  "http://127.0.0.1:8080/download?fileId=<fileId>&fileName=test.pdf" \
  -o test.pdf
```

Gateway sẽ gọi agent local:

```text
GET AGENT_URL?fileId=<fileId>&downloadFilePublic=true
```

Headers gửi sang agent:

```text
Accept: application/octet-stream, application/pdf, */*
Origin: https://muasamcong.mpi.gov.vn
Referer: https://muasamcong.mpi.gov.vn/web/guest/contractor-selection
User-Agent: Mozilla/5.0
```

## Bảo Mật

- `/download` bắt buộc header `X-Api-Key`.
- Không log API key.
- Không expose Gateway ra internet.
- Windows Firewall chỉ nên allow IP backend/K8s gọi TCP 8080.
- Gateway không ghi file xuống ổ đĩa.

## Kiểm Tra Cú Pháp

```bash
npm run check
```

## Test Từ Máy Khác

```bash
curl http://WINDOWS_IP:8080/health
```

```bash
curl -L \
  -H "X-Api-Key: secret" \
  "http://WINDOWS_IP:8080/download?fileId=<fileId>&fileName=test.pdf" \
  -o test.pdf
```

File tải về phải size > 0 và mở được.
