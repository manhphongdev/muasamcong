# Kế Hoạch Download File Qua Windows Gateway

## 1. Mục Tiêu

Triển khai luồng tải file trong đó file chỉ có thể tải qua Windows agent, được Windows Gateway proxy dạng stream, sau đó backend nhận stream và lưu file.

Mục tiêu trước mắt:

- Chạy local/dev trước.
- Chưa deploy Kubernetes.
- Windows Gateway không lưu file.
- Backend là bên duy nhất ghi file xuống storage.
- SMB và Kubernetes làm ở các phase sau.

Luồng tổng quát:

```text
Backend/Monitor
  -> Windows Gateway
  -> Windows local muasamcong agent
  -> stream file ngược về
  -> Backend ghi file vào DOWNLOAD_ROOT
```

## 2. Kiến Trúc

```text
+-------------------+        HTTP        +------------------+        HTTP local        +----------------+
| Backend / Monitor | -----------------> | Windows Gateway  | ----------------------> | Windows Agent  |
| Java Spring Boot  |                    | Node.js 20 LTS   |                         | 127.0.0.1:1234 |
+-------------------+                    +------------------+                         +----------------+
        |
        | ghi file
        v
+-------------------+
| DOWNLOAD_ROOT     |
| local disk / SMB  |
+-------------------+
```

Trách nhiệm từng thành phần:

- Windows Agent: downloader local hiện có, truy cập được file của muasamcong.
- Windows Gateway: proxy có xác thực, chỉ stream file từ Windows Agent về backend.
- Backend: gọi Gateway, nhận stream, ghi file xuống ổ đĩa/SMB, cập nhật DB/monitor.

Không làm trong MVP:

- Gateway lưu file local.
- Gateway truy cập SMB.
- Deploy Kubernetes.
- Retry vô hạn trong download service.

## 3. Phase 1: Windows Gateway MVP

### 3.1 Deliverables

Tạo thư mục:

```text
windows-gateway/
  package.json
  server.js
  README.md
```

Runtime:

- Node.js 20 LTS.
- Gateway chạy trên máy Windows.
- Gateway chỉ expose trong mạng nội bộ/private network.

### 3.2 Biến Môi Trường

```text
PORT=8080
API_KEY=<secret>
AGENT_URL=http://127.0.0.1:1234/api/download/file/browser/public
DOWNLOAD_TIMEOUT_MS=120000
```

### 3.3 API: GET /health

Response:

```json
{
  "status": "OK",
  "agentUrl": "http://127.0.0.1:1234/api/download/file/browser/public",
  "mode": "STREAM_ONLY"
}
```

Trong MVP, `/health` không bắt buộc xác thực. Nếu cần siết bảo mật sau này, có thể bắt `X-Api-Key` cho mọi route.

### 3.4 API: GET /download

Request:

```http
GET /download?fileId=<fileId>&fileName=<fileName>
X-Api-Key: <secret>
```

Bắt buộc:

- Header `X-Api-Key`.
- Query param `fileId`.

Không bắt buộc:

- Query param `fileName`. Nếu thiếu hoặc không hợp lệ, dùng fallback `download.bin`.

Gateway gọi Windows agent local:

```text
GET {AGENT_URL}?fileId={encoded fileId}&downloadFilePublic=true
```

Headers gửi sang agent:

```text
Accept: application/octet-stream, application/pdf, */*
Origin: https://muasamcong.mpi.gov.vn
Referer: https://muasamcong.mpi.gov.vn/web/guest/contractor-selection
User-Agent: Mozilla/5.0
```

### 3.5 Validate Và Sanitize Ở Gateway

`fileId`:

- Không được rỗng.
- Trim whitespace.
- URL-encode trước khi gửi sang agent.

`fileName`:

- Loại bỏ path separator `/` và `\`.
- Thay ký tự không hợp lệ trên Windows bằng `_`:

```text
< > : " / \ | ? *
```

- Loại bỏ control characters.
- Trim whitespace.
- Nếu sau sanitize bị rỗng thì dùng `download.bin`.

### 3.6 Xử Lý Lỗi Ở Gateway

Thiếu hoặc sai API key:

```http
401 Unauthorized
Content-Type: application/json
```

Thiếu `fileId`:

```http
400 Bad Request
Content-Type: application/json
```

Agent trả HTTP không phải 2xx:

```http
502 Bad Gateway
Content-Type: application/json
```

Agent trả `Content-Type` bắt đầu bằng `text/html` hoặc `application/json`:

```http
502 Bad Gateway
Content-Type: application/json
```

Timeout:

```http
504 Gateway Timeout
Content-Type: application/json
```

### 3.7 Khi Gateway Thành Công

Khi agent trả binary hợp lệ:

- Stream body từ agent trực tiếp về client/backend.
- Không buffer toàn bộ file vào memory.
- Không ghi file xuống ổ đĩa Gateway.
- Preserve header an toàn nếu có:
  - `Content-Type`
  - `Content-Length`
- Set header:

```http
Content-Disposition: attachment; filename="<sanitizedFileName>"
```

### 3.8 Log Ở Gateway

Log một dòng cho mỗi request download:

```text
fileId=<fileId> status=<http status> durationMs=<durationMs>
```

Không log:

- API key.
- Response body.
- Cookie/token nhạy cảm.

## 4. Phase 2: Test Gateway Từ Máy Khác

### 4.1 Windows Firewall

Mở TCP port `8080` trên Windows, chỉ allow IP backend/dev được gọi.

Ví dụ rule mong muốn:

```text
Allow inbound TCP 8080 from <backend-ip>
Deny public access / không expose internet
```

### 4.2 Test Health

Từ máy backend/dev:

```bash
curl http://WINDOWS_IP:8080/health
```

Kết quả mong muốn:

```json
{
  "status": "OK",
  "agentUrl": "http://127.0.0.1:1234/api/download/file/browser/public",
  "mode": "STREAM_ONLY"
}
```

### 4.3 Test Download

```bash
curl -L \
  -H "X-Api-Key: secret" \
  "http://WINDOWS_IP:8080/download?fileId=<fileId>&fileName=test.pdf" \
  -o test.pdf
```

Kết quả mong muốn:

- HTTP 200.
- File `test.pdf` tồn tại.
- File size > 0.
- File mở được.

## 5. Phase 3: Backend Download Local

### 5.1 Deliverables Backend

Thêm các file backend:

```text
src/main/java/com/muasamcong/config/DownloadProperties.java
src/main/java/com/muasamcong/enums/FileDownloadStatus.java
src/main/java/com/muasamcong/dto/download/FileDownloadRequest.java
src/main/java/com/muasamcong/dto/download/FileDownloadResult.java
src/main/java/com/muasamcong/service/download/FileDownloadService.java
src/main/java/com/muasamcong/service/download/impl/WindowsGatewayFileDownloadService.java
```

Endpoint dev/test tùy chọn:

```text
src/main/java/com/muasamcong/controller/FileDownloadController.java
```

### 5.2 Config Backend

Thêm vào `application.yaml`:

```yaml
download:
  windows-gateway-url: ${WINDOWS_GATEWAY_URL:http://127.0.0.1:8080}
  windows-gateway-api-key: ${WINDOWS_GATEWAY_API_KEY:}
  root: ${DOWNLOAD_ROOT:./downloads}
  timeout-ms: ${DOWNLOAD_TIMEOUT_MS:120000}
```

Env runtime:

```text
WINDOWS_GATEWAY_URL=http://WINDOWS_IP:8080
WINDOWS_GATEWAY_API_KEY=<secret>
DOWNLOAD_ROOT=./downloads
DOWNLOAD_TIMEOUT_MS=120000
```

### 5.3 Input Của Backend Service

Signature đề xuất:

```java
FileDownloadResult download(String fileId, String fileName, String relativePath);
```

Ý nghĩa input:

- `fileId`: bắt buộc.
- `fileName`: bắt buộc, sẽ sanitize trước khi lưu.
- `relativePath`: thư mục con dưới `DOWNLOAD_ROOT`, phải sanitize.

Ví dụ request:

```json
{
  "fileId": "abc123",
  "fileName": "decision.pdf",
  "relativePath": "IB2500000001/kqlcnt"
}
```

### 5.4 Result Model

Các trạng thái:

```text
SUCCESS
SKIPPED_EXISTS
FAILED
```

Các field trả về:

```text
status
fileId
fileName
relativePath
storagePath
size
message
```

### 5.5 Flow Backend Download

1. Validate `fileId` không rỗng.
2. Sanitize `fileName`.
3. Sanitize `relativePath`.
4. Resolve root:

```text
root = DOWNLOAD_ROOT normalized absolute path
```

5. Build target path:

```text
target = root / relativePath / fileName
```

6. Normalize target path.
7. Reject nếu target không nằm dưới root.
8. Nếu target đã tồn tại và size > 0:

```text
return SKIPPED_EXISTS
```

9. Tạo parent directories nếu chưa có.
10. Xóa file `.part` cũ nếu còn:

```text
target.part
```

11. Gọi Gateway:

```http
GET {WINDOWS_GATEWAY_URL}/download?fileId=<encoded>&fileName=<encoded>
X-Api-Key: <secret>
```

12. Nếu HTTP status không phải 2xx:

```text
cleanup .part
return FAILED
```

13. Nếu response `Content-Type` là JSON hoặc HTML:

```text
cleanup .part
return FAILED
```

14. Stream response body vào `target.part`.
15. Kiểm tra `target.part` size > 0.
16. Rename/move:

```text
target.part -> target
```

17. Verify target size > 0.
18. Trả về:

```text
SUCCESS
```

### 5.6 Bảo Vệ Path Ở Backend

Sanitize `fileName`:

- Loại bỏ `/` và `\`.
- Thay ký tự không hợp lệ bằng `_`.
- Loại bỏ control characters.
- Trim.
- Fallback `download.bin` nếu rỗng.

Sanitize `relativePath`:

- Không cho path segment `..`.
- Không cho ký tự `:`.
- Không cho absolute path.
- Normalize separator.
- Check final target path phải startsWith normalized `DOWNLOAD_ROOT`.

### 5.7 Yêu Cầu Memory

Không load toàn bộ file vào memory.

Dùng streaming:

- `java.net.http.HttpClient`
- `BodyHandlers.ofInputStream()`
- Copy từ `InputStream` sang `OutputStream` theo buffer.

## 6. Phase 4: Gắn Download Vào Monitor

### 6.1 Dữ Liệu Hiện Có

Model `BiddingResultSummary` hiện có:

```text
decisionFileId
decisionFileName
evalReportFileInfo
```

Model `BiddingDocument` hiện có:

```text
contractInfo
fileExternalId
fileName
fileHash
fileType
storagePath
importStatus
importedAt
```

### 6.2 Extract File Từ KQLCNT

Sau khi sync KQLCNT xong, extract các file:

```text
decisionFileId / decisionFileName
reportFileId / reportFileName nếu xác nhận có trong payload
evalReportFileInfo[].fileId / fileName nếu evalReportFileInfo là JSON array
```

Nếu `evalReportFileInfo` chưa ổn định format, log và skip parsing cho đến khi có payload thật để chốt.

### 6.3 Quy Ước Relative Path

Đề xuất:

```text
<notifyNo>/kqlcnt
```

Ví dụ:

```text
IB2500000001/kqlcnt/decision.pdf
IB2500000001/kqlcnt/eval-report.pdf
```

Sau này nếu tải file TBMT:

```text
<notifyNo>/tbmt
```

### 6.4 Lưu Trạng Thái File

Dùng `BiddingDocument.importStatus`:

```text
PENDING
SUCCESS
SKIPPED_EXISTS
FAILED
```

Nên bổ sung field:

```text
lastError
```

Lý do:

- Monitor cần biết file nào fail vì sao.
- `lastError` chỉ lưu message ngắn, an toàn.

### 6.5 Flow Download Trong Monitor

Với mỗi file extract được:

1. Upsert `BiddingDocument` theo `contractInfo + fileExternalId`.
2. Set `importStatus = PENDING` trước khi download.
3. Gọi backend `FileDownloadService`.
4. Nếu `SUCCESS`:
   - `importStatus = SUCCESS`
   - `storagePath = result.storagePath`
   - `importedAt = now`
5. Nếu `SKIPPED_EXISTS`:
   - `importStatus = SKIPPED_EXISTS`
   - `storagePath = result.storagePath`
6. Nếu `FAILED`:
   - `importStatus = FAILED`
   - `lastError = result.message`

Không retry vô hạn trong service.

Monitor cycle sau sẽ quyết định xử lý lại `FAILED` nếu cần.

## 7. Phase 5: Thêm SMB Sau

Không cần đổi logic code.

Chỉ đổi `DOWNLOAD_ROOT`:

```text
DOWNLOAD_ROOT=/mnt/muasamcong-downloads
```

Backend vẫn là bên ghi file.

Windows Gateway vẫn không cần quyền SMB và không cần biết SMB tồn tại.

## 8. Phase 6: Kubernetes Sau Cùng

Deploy backend lên K8s.

Set env:

```text
WINDOWS_GATEWAY_URL=http://WINDOWS_IP:8080
WINDOWS_GATEWAY_API_KEY=<secret>
DOWNLOAD_ROOT=/mnt/muasamcong-downloads
DOWNLOAD_TIMEOUT_MS=120000
```

Network cần mở:

```text
K8s -> Windows Gateway TCP 8080
K8s -> SMB TCP 445 nếu dùng SMB
```

Storage:

- Mount SMB vào backend pod nếu dùng SMB.
- Gateway vẫn stateless.

## 9. Yêu Cầu Bảo Mật

Gateway:

- Chỉ expose nội bộ/private network.
- Bắt buộc `X-Api-Key` cho `/download`.
- Không expose internet.
- Windows firewall chỉ allow IP backend/K8s gọi port `8080`.
- Không lưu file.
- Không log API key.

Backend:

- API key đặt trong env, không hardcode.
- Sanitize `fileName`.
- Sanitize `relativePath`.
- Chống path traversal.
- Dùng timeout khi gọi Gateway.
- Cleanup file `.part` khi lỗi.

## 10. Checklist Test

### 10.1 Test Gateway Local Trên Windows

```bash
cd windows-gateway
npm install
set PORT=8080
set API_KEY=secret
set AGENT_URL=http://127.0.0.1:1234/api/download/file/browser/public
set DOWNLOAD_TIMEOUT_MS=120000
npm start
```

Health:

```bash
curl http://127.0.0.1:8080/health
```

Download:

```bash
curl -L \
  -H "X-Api-Key: secret" \
  "http://127.0.0.1:8080/download?fileId=<fileId>&fileName=test.pdf" \
  -o test.pdf
```

### 10.2 Test Gateway Từ Máy Backend

```bash
curl http://WINDOWS_IP:8080/health
```

```bash
curl -L \
  -H "X-Api-Key: secret" \
  "http://WINDOWS_IP:8080/download?fileId=<fileId>&fileName=test.pdf" \
  -o test.pdf
```

### 10.3 Test Backend Service

Nếu thêm endpoint dev/test:

```bash
curl -X POST "http://127.0.0.1:8888/api/v1/downloads/files" \
  -H "Content-Type: application/json" \
  -d '{
    "fileId": "<fileId>",
    "fileName": "test.pdf",
    "relativePath": "IB2500000001/kqlcnt"
  }'
```

Kết quả thành công kỳ vọng:

```json
{
  "status": "SUCCESS",
  "fileId": "<fileId>",
  "fileName": "test.pdf",
  "relativePath": "IB2500000001/kqlcnt",
  "storagePath": ".../downloads/IB2500000001/kqlcnt/test.pdf",
  "size": 12345,
  "message": "Downloaded"
}
```

Chạy lần hai với cùng file nên trả:

```json
{
  "status": "SKIPPED_EXISTS"
}
```

## 11. Thứ Tự Implement Đề Xuất

1. Tạo Windows Gateway files.
2. Test Gateway local trên Windows bằng `fileId` thật.
3. Test Gateway từ máy backend qua LAN.
4. Implement backend download service.
5. Thêm endpoint dev/test nếu cần test thủ công.
6. Test backend download qua Gateway.
7. Extract file từ KQLCNT result.
8. Upsert `BiddingDocument` và lưu trạng thái file.
9. Thêm monitor summary/details cho trạng thái file.
10. Thêm SMB bằng cách đổi `DOWNLOAD_ROOT`.
11. Deploy backend lên K8s khi network/storage sẵn sàng.

## 12. Các Quyết Định Cần Chốt

1. Có thêm endpoint dev/test `POST /downloads/files` để test thủ công không?
2. Có thêm `lastError` vào `BiddingDocument` ngay không?
3. Format thực tế của `evalReportFileInfo` trong payload production là gì?
4. `/health` có cần bắt API key ở phase sau không?
5. Gateway nên bind `127.0.0.1`, `0.0.0.0`, hay một Windows LAN IP cụ thể?
