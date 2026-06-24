import React, { useEffect, useState } from 'react';
import {
  AlertTriangle,
  Clipboard,
  ExternalLink,
  FileText,
  FolderOpen,
  X
} from 'lucide-react';
import { DocumentFileItem, DocumentSummary, PackageData } from '../types';
import { formatDateTime } from '../utils/date';
import { Badge } from './ui/Badge';
import { Button } from './ui/Button';
import { Card, CardContent, CardHeader } from './ui/Card';

interface TenderDetailDrawerProps {
  pkg: PackageData | null;
  isOpen: boolean;
  onClose: () => void;
  onTriggerSyncSingle: (notifyNo: string) => void;
  isSyncing: boolean;
  apiBaseUrl: string;
  onOpenFolder: (folderPath: string) => void;
  onCopyFolder: (folderPath: string) => void;
}

interface DetailRowProps {
  label: string;
  value: React.ReactNode;
}

function DetailRow({ label, value }: DetailRowProps) {
  return (
    <div className="grid gap-1 border-b border-slate-100 px-4 py-3 last:border-b-0 md:grid-cols-[220px_minmax(0,1fr)] md:gap-4">
      <dt className="text-xs font-medium text-slate-500">{label}</dt>
      <dd className="min-w-0 text-sm leading-6 text-slate-900">{value}</dd>
    </div>
  );
}

function formatVnd(value?: number) {
  if (!value) return 'Chưa có';
  return `${value.toLocaleString('vi-VN')} VND`;
}

function getExecutionTime(pkg: PackageData) {
  if (pkg.winningContractor) {
    const winner = pkg.bidders?.find((bidder) => bidder.name === pkg.winningContractor);
    if (winner?.executionTimeText) return winner.executionTimeText;
  }
  return pkg.bidders?.[0]?.executionTimeText || 'Chưa có';
}

function getOpenPathLabel(path: string) {
  const cleaned = path.trim().split(/[?#]/)[0];
  const lastSegment = cleaned.split(/[\\/]/).pop() || '';
  return /\.[A-Za-z0-9]{1,10}$/.test(lastSegment) ? 'Mở file' : 'Mở thư mục';
}

function normalizeText(value: string) {
  return value
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd');
}

function formatBytes(value: number | null) {
  if (!value) return '--';
  if (value < 1024) return `${value} B`;
  if (value < 1024 * 1024) return `${Math.round(value / 1024)} KB`;
  return `${(value / 1024 / 1024).toFixed(1)} MB`;
}

function documentStatusTone(status: string): 'success' | 'warning' | 'danger' | 'neutral' {
  const normalized = status.toUpperCase();
  if (normalized === 'SUCCESS' || normalized === 'DOWNLOADED') return 'success';
  if (normalized === 'FAILED') return 'danger';
  if (normalized === 'PENDING' || normalized === 'DOWNLOADING') return 'warning';
  return 'neutral';
}

function documentViewUrl(apiBaseUrl: string, file: DocumentFileItem) {
  return `${apiBaseUrl}/documents/files/${file.id}/view`;
}

const documentGroups = [
  {
    id: 'bidOpening',
    title: 'Biên bản mở thầu',
    match: (file: DocumentFileItem) => {
      const text = normalizeText(`${file.fileType || ''} ${file.fileName || ''} ${file.storagePath || ''}`);
      return text.includes('bid_opening') || text.includes('bbmt') || text.includes('bien ban mo thau');
    }
  },
  {
    id: 'petition',
    title: 'Kiến nghị',
    match: (file: DocumentFileItem) => {
      const text = normalizeText(`${file.fileType || ''} ${file.fileName || ''} ${file.storagePath || ''}`);
      return text.includes('petition') || text.includes('kien nghi');
    }
  },
  {
    id: 'clarification',
    title: 'Làm rõ',
    match: (file: DocumentFileItem) => {
      const text = normalizeText(`${file.fileType || ''} ${file.fileName || ''} ${file.storagePath || ''}`);
      return text.includes('clarification') || text.includes('clarify') || text.includes('lam ro');
    }
  },
  {
    id: 'goods',
    title: 'Bảng dự thầu hàng hoá',
    match: (file: DocumentFileItem) => {
      const text = normalizeText(`${file.fileType || ''} ${file.fileName || ''} ${file.storagePath || ''}`);
      return text.includes('goods_excel') || text.includes('bang du thau') || text.includes('hang hoa');
    }
  },
  {
    id: 'kqlcnt',
    title: 'Quyết định phê duyệt KQLCNT nhà thầu',
    match: (file: DocumentFileItem) => {
      const text = normalizeText(`${file.fileType || ''} ${file.fileName || ''} ${file.storagePath || ''}`);
      return text.includes('kqlcnt_decision') || text.includes('kqlcnt') || text.includes('quyet dinh phe duyet');
    }
  }
] as const;

type DocumentSection = {
  id: string;
  title: string;
  files: DocumentFileItem[];
};

function groupDownloadedDocuments(files: DocumentFileItem[]) {
  const downloaded = files.filter((file) => Boolean(file.storagePath?.trim()));
  const used = new Set<number>();
  const groups: DocumentSection[] = documentGroups.map((group) => {
    const items = downloaded.filter((file) => {
      if (used.has(file.id)) return false;
      const matched = group.match(file);
      if (matched) used.add(file.id);
      return matched;
    });
    return { id: group.id, title: group.title, files: items };
  });
  const otherFiles = downloaded.filter((file) => !used.has(file.id));
  if (otherFiles.length) {
    groups.push({ id: 'other', title: 'Khác', files: otherFiles });
  }
  return groups;
}

export default function TenderDetailDrawer({
  pkg,
  isOpen,
  onClose,
  apiBaseUrl,
  onOpenFolder,
  onCopyFolder
}: TenderDetailDrawerProps) {
  const [activeTab, setActiveTab] = useState<'INFO' | 'DOCUMENTS'>('INFO');
  const [documentSummary, setDocumentSummary] = useState<DocumentSummary | null>(null);
  const [isDocumentsLoading, setIsDocumentsLoading] = useState(false);
  const [documentsError, setDocumentsError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) setActiveTab('INFO');
  }, [isOpen, pkg]);

  useEffect(() => {
    if (!isOpen || activeTab !== 'DOCUMENTS' || !pkg?.notifyNo) return;

    const controller = new AbortController();
    setIsDocumentsLoading(true);
    setDocumentsError(null);

    fetch(`${apiBaseUrl}/documents/summary/${encodeURIComponent(pkg.notifyNo)}`, { signal: controller.signal })
      .then(async (response) => {
        const body = await response.json().catch(() => null) as { success?: boolean; message?: string; data?: DocumentSummary } | null;
        if (!response.ok || body?.success === false || !body?.data) {
          throw new Error(body?.message || `HTTP ${response.status}`);
        }
        setDocumentSummary(body.data);
      })
      .catch((error) => {
        if (error instanceof DOMException && error.name === 'AbortError') return;
        setDocumentsError(error instanceof Error ? error.message : 'Không thể tải danh sách tài liệu');
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsDocumentsLoading(false);
      });

    return () => controller.abort();
  }, [activeTab, apiBaseUrl, isOpen, pkg?.notifyNo]);

  if (!isOpen || !pkg) return null;

  const documentSections = documentSummary ? groupDownloadedDocuments(documentSummary.files || []) : [];
  const downloadedFileCount = documentSections.reduce((total, section) => total + section.files.length, 0);

  return (
    <>
      <div
        onClick={onClose}
        className="fixed inset-0 z-[120] cursor-pointer bg-slate-950/40"
        id="tender-drawer-backdrop"
      />

      <aside
        className="fixed right-0 top-0 z-[130] flex h-full w-full flex-col border-l border-slate-200 bg-slate-50 shadow-lg md:w-[860px] xl:w-[1080px]"
        id="tender-details-drawer"
      >
        <header className="border-b border-slate-200 bg-white px-5 py-4">
          <div className="flex items-start justify-between gap-4">
            <div className="min-w-0">
              <h2 className="line-clamp-2 text-base font-semibold leading-6 text-slate-950">{pkg.title}</h2>
              <p className="mt-1 truncate text-sm text-slate-500">{pkg.investor}</p>
            </div>

            <div className="flex shrink-0 items-center gap-2">
              {pkg.bidUrl ? (
                <Button size="sm" onClick={() => window.open(pkg.bidUrl!, '_blank')} title="Mở link gói thầu">
                  <ExternalLink className="h-3.5 w-3.5" />
                </Button>
              ) : null}
              <button
                type="button"
                onClick={onClose}
                className="rounded-md p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-900"
                aria-label="Đóng chi tiết"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
          </div>
        </header>

        <div className="border-b border-slate-200 bg-white px-5">
          <div className="flex gap-1">
            <button
              type="button"
              onClick={() => setActiveTab('INFO')}
              className={`border-b-2 px-3 py-3 text-sm font-medium transition-colors ${
                activeTab === 'INFO'
                  ? 'border-slate-950 text-slate-950'
                  : 'border-transparent text-slate-500 hover:text-slate-900'
              }`}
            >
              Thông tin
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('DOCUMENTS')}
              className={`border-b-2 px-3 py-3 text-sm font-medium transition-colors ${
                activeTab === 'DOCUMENTS'
                  ? 'border-slate-950 text-slate-950'
                  : 'border-transparent text-slate-500 hover:text-slate-900'
              }`}
            >
              Tài liệu
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto p-5">
          {activeTab === 'INFO' ? (
            <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_320px]">
              <Card>
                <CardHeader>
                  <h3 className="text-sm font-semibold text-slate-950">Thông tin gói thầu</h3>
                </CardHeader>
                <dl>
                  <DetailRow label="Số TBMT" value={<span className="font-mono font-medium">{pkg.notifyNo}</span>} />
                  <DetailRow label="Tên gói thầu" value={pkg.title} />
                  <DetailRow label="Chủ đầu tư" value={pkg.investor} />
                  <DetailRow label="Dự toán" value={<span className="font-medium">{formatVnd(pkg.budget)}</span>} />
                  <DetailRow label="Giá trúng thầu" value={pkg.winningPrice ? <span className="font-medium text-emerald-700">{formatVnd(pkg.winningPrice)}</span> : <span className="text-slate-400">Chưa công bố</span>} />
                  <DetailRow label="Nhà thầu trúng thầu" value={pkg.winningContractor || <span className="text-slate-400">Chưa xác định</span>} />
                  <DetailRow label="Ngày đăng tải" value={pkg.publishDate || '--'} />
                  <DetailRow label="Thời điểm đóng thầu" value={formatDateTime(pkg.closeTime)} />
                  <DetailRow label="Thời gian thực hiện" value={getExecutionTime(pkg)} />
                </dl>
              </Card>

              <Card>
                <CardHeader>
                  <h3 className="text-sm font-semibold text-slate-950">Thư mục THHD</h3>
                </CardHeader>
                <CardContent className="space-y-3">
                  {pkg.folderPath ? (
                    <div className="flex flex-wrap gap-2">
                      <Button size="sm" onClick={() => onOpenFolder(pkg.folderPath!)}>
                        <FolderOpen className="h-3.5 w-3.5" />
                        {getOpenPathLabel(pkg.folderPath!)}
                      </Button>
                      <Button size="sm" onClick={() => onCopyFolder(pkg.folderPath!)}>
                        <Clipboard className="h-3.5 w-3.5" />
                        Sao chép
                      </Button>
                    </div>
                  ) : (
                    <p className="text-sm text-slate-500">Chưa cấu hình thư mục lưu trữ cho gói thầu này.</p>
                  )}
                </CardContent>
              </Card>
            </div>
          ) : (
            <Card>
              <CardHeader>
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <h3 className="text-sm font-semibold text-slate-950">Tài liệu</h3>
                    {documentSummary ? (
                      <p className="mt-1 text-xs text-slate-500">
                        {downloadedFileCount} file đã tải có đường dẫn / {documentSummary.total} file trong hệ thống
                      </p>
                    ) : null}
                  </div>
                  {documentSummary ? (
                    <div className="flex flex-wrap gap-2">
                      <Badge tone="success">Đã tải {documentSummary.success}</Badge>
                      <Badge tone="warning">Chờ {documentSummary.pending + documentSummary.downloading}</Badge>
                      <Badge tone={documentSummary.failed > 0 ? 'danger' : 'neutral'}>Lỗi {documentSummary.failed}</Badge>
                    </div>
                  ) : null}
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                {isDocumentsLoading ? (
                  <div className="space-y-3">
                    {[0, 1, 2].map((item) => (
                      <div key={item} className="h-20 animate-pulse rounded-lg bg-slate-100" />
                    ))}
                  </div>
                ) : documentsError ? (
                  <div className="flex items-center gap-3 rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700">
                    <AlertTriangle className="h-4 w-4 shrink-0" />
                    {documentsError}
                  </div>
                ) : downloadedFileCount === 0 ? (
                  <div className="rounded-lg border border-dashed border-slate-200 bg-slate-50 p-10 text-center text-sm text-slate-500">
                    Chưa có file đã tải có đường dẫn lưu trữ.
                  </div>
                ) : (
                  documentSections.map((section) => (
                    <section key={section.id} className="rounded-lg border border-slate-200 bg-white">
                      <div className="flex items-center justify-between gap-3 border-b border-slate-100 px-4 py-3">
                        <h4 className="text-sm font-semibold text-slate-900">{section.title}</h4>
                        <Badge>{section.files.length}</Badge>
                      </div>
                      {section.files.length === 0 ? (
                        <p className="px-4 py-4 text-sm text-slate-400">Chưa có file trong nhóm này.</p>
                      ) : (
                        <div className="divide-y divide-slate-100">
                          {section.files.map((file) => (
                            <div key={file.id} className="grid gap-3 px-4 py-3 lg:grid-cols-[minmax(0,1fr)_auto] lg:items-center">
                              <div className="min-w-0">
                                <div className="flex flex-wrap items-center gap-2">
                                  <FileText className="h-4 w-4 shrink-0 text-slate-400" />
                                  <p className="min-w-0 truncate text-sm font-semibold text-slate-900">{file.fileName || file.fileId}</p>
                                  <Badge tone={documentStatusTone(file.downloadStatus)}>{file.downloadStatus}</Badge>
                                  <span className="text-xs text-slate-400">{formatBytes(file.fileSize)}</span>
                                </div>
                                <p className="mt-1 break-all font-mono text-xs leading-5 text-slate-500">{file.storagePath}</p>
                              </div>
                              <div className="flex shrink-0 flex-wrap gap-2">
                                <Button size="sm" onClick={() => window.open(documentViewUrl(apiBaseUrl, file), '_blank', 'noopener,noreferrer')}>
                                  <ExternalLink className="h-3.5 w-3.5" />
                                  Mở tab
                                </Button>
                                <Button size="sm" onClick={() => onCopyFolder(file.storagePath!)}>
                                  <Clipboard className="h-3.5 w-3.5" />
                                  Sao chép
                                </Button>
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </section>
                  ))
                )}
              </CardContent>
            </Card>
          )}
        </div>
      </aside>
    </>
  );
}
