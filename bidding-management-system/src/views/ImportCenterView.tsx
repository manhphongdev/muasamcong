import { useEffect, useState } from 'react';
import { AlertTriangle, CloudUpload, FolderInput, Loader2, RefreshCw } from 'lucide-react';
import { formatVietnamDateTime } from '../utils/date';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Card, CardContent, CardHeader } from '../components/ui/Card';
import { Textarea } from '../components/ui/Input';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || `${window.location.protocol}//${window.location.hostname}:8888/api/v1`;

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

interface RootFolderItem {
  id: number;
  path: string;
  status: 'ACTIVE' | 'INACTIVE' | 'DELETED';
  lastImportedAt: string | null;
  lastStatus: string | null;
  lastError: string | null;
  createdAt: string;
  updatedAt: string;
}

interface DuplicatePackageItem {
  notifyNo: string;
  folderName: string;
  newPath: string;
  existingPath: string | null;
  message: string;
}

interface RootFolderCreateResult {
  folders: RootFolderItem[];
  duplicates: DuplicatePackageItem[];
}

function parseFolderPaths(rawText: string): string[] {
  return rawText
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean);
}

export default function ImportCenterView() {
  const [pastedContent, setPastedContent] = useState('');
  const [isValidating, setIsValidating] = useState(false);
  const [rootFolders, setRootFolders] = useState<RootFolderItem[]>([]);
  const [duplicatePackages, setDuplicatePackages] = useState<DuplicatePackageItem[]>([]);
  const [isLoadingRootFolders, setIsLoadingRootFolders] = useState(false);

  const fetchRootFolders = async () => {
    setIsLoadingRootFolders(true);
    try {
      const response = await fetch(`${API_BASE_URL}/bid-package-sync-root-folders`);
      const body = await response.json() as ApiResponse<RootFolderItem[]>;
      if (!response.ok || !body.success) {
        throw new Error(body.message || `HTTP ${response.status}`);
      }
      setRootFolders(body.data || []);
      setDuplicatePackages([]);
    } catch (error) {
      window.alert(`Không tải được danh sách thư mục đã lưu: ${error instanceof Error ? error.message : 'Không rõ lỗi'}`);
    } finally {
      setIsLoadingRootFolders(false);
    }
  };

  useEffect(() => {
    fetchRootFolders();
  }, []);

  const handleImportParentFolders = async () => {
    const paths = parseFolderPaths(pastedContent);
    if (paths.length === 0) {
      window.alert('Nhập ít nhất một đường dẫn thư mục cần lưu.');
      return;
    }

    setIsValidating(true);
    try {
      const saveResponse = await fetch(`${API_BASE_URL}/bid-package-sync-root-folders`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ paths })
      });
      const saveBody = await saveResponse.json() as ApiResponse<RootFolderCreateResult>;
      if (!saveResponse.ok || !saveBody.success) {
        throw new Error(saveBody.message || `HTTP ${saveResponse.status}`);
      }

      setRootFolders(saveBody.data?.folders || []);
      setDuplicatePackages(saveBody.data?.duplicates || []);
      setPastedContent('');
      window.alert(`Đã lưu ${paths.length} thư mục.`);
    } catch (error) {
      window.alert(`Lưu thư mục thất bại: ${error instanceof Error ? error.message : 'Không rõ lỗi'}`);
    } finally {
      setIsValidating(false);
    }
  };

  return (
    <div className="space-y-4" id="view-import-center">
      <div className="grid grid-cols-1 gap-4 xl:grid-cols-[390px_minmax(0,1fr)]">
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <FolderInput className="h-4 w-4 text-slate-700" />
              <h2 className="text-sm font-semibold text-slate-950">Thêm thư mục gói thầu</h2>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <label className="text-xs font-medium text-slate-600">Danh sách đường dẫn</label>
                <button
                  type="button"
                  onClick={() => setPastedContent('C:/Projects/Bidding/Package_IB2600058201\nC:/Projects/Bidding/Package_IB2600006452')}
                  className="text-xs font-medium text-slate-600 hover:text-slate-950"
                >
                  Dán mẫu
                </button>
              </div>
              <Textarea
                rows={11}
                value={pastedContent}
                onChange={(event) => setPastedContent(event.target.value)}
                placeholder={'Ví dụ:\nC:/Projects/Bidding/Package_01\nC:/Projects/Bidding/Package_02'}
                className="min-h-[220px] resize-none font-mono text-xs leading-relaxed"
              />
            </div>

            <Button
              variant="primary"
              className="w-full"
              onClick={handleImportParentFolders}
              disabled={isValidating || !pastedContent.trim()}
            >
              {isValidating ? <Loader2 className="h-4 w-4 animate-spin" /> : <CloudUpload className="h-4 w-4" />}
              Lưu thư mục
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between gap-3">
            <div>
              <h3 className="text-sm font-semibold text-slate-950">Thư mục đã lưu</h3>
              <p className="mt-1 text-xs text-slate-500">{rootFolders.length} thư mục trong hệ thống.</p>
            </div>
            <Button size="sm" onClick={fetchRootFolders} disabled={isLoadingRootFolders}>
              <RefreshCw className={`h-3.5 w-3.5 ${isLoadingRootFolders ? 'animate-spin' : ''}`} />
              Làm mới
            </Button>
          </CardHeader>
          <CardContent>
            {rootFolders.length === 0 ? (
              <div className="rounded-lg border border-dashed border-slate-200 bg-slate-50 p-8 text-center text-sm text-slate-500">
                Chưa có thư mục nào được lưu.
              </div>
            ) : (
              <div className="max-h-[420px] overflow-y-auto rounded-lg border border-slate-200">
                <div className="divide-y divide-slate-100">
                  {rootFolders.map((folder, index) => {
                    const isActive = folder.status === 'ACTIVE';
                    return (
                      <div key={folder.id} className="flex flex-col gap-3 p-3 hover:bg-slate-50 md:flex-row md:items-center md:justify-between">
                        <div className="min-w-0">
                          <div className="flex items-center gap-2">
                            <span className="font-mono text-xs text-slate-400">#{index + 1}</span>
                            <p className="truncate font-mono text-sm font-medium text-slate-900" title={folder.path}>{folder.path}</p>
                          </div>
                          <p className="mt-1 text-xs text-slate-500">
                            {folder.lastImportedAt ? `Lần lưu cuối: ${formatVietnamDateTime(folder.lastImportedAt)}` : 'Chưa có lần lưu dữ liệu'}
                            {folder.lastError ? ` - ${folder.lastError}` : ''}
                          </p>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {duplicatePackages.length > 0 && (
        <Card className="border-amber-200 bg-amber-50">
          <CardHeader className="border-amber-200">
            <div className="flex items-start gap-2">
              <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-600" />
              <div>
                <h3 className="text-sm font-semibold text-amber-950">Phát hiện {duplicatePackages.length} gói thầu trùng</h3>
                <p className="mt-1 text-xs text-amber-800">Các gói đã tồn tại sẽ không được sync lại từ thư mục mới.</p>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <div className="grid gap-2 md:grid-cols-2">
              {duplicatePackages.map((item) => (
                <div key={`${item.notifyNo}-${item.newPath}`} className="rounded-lg border border-amber-200 bg-white p-3 text-xs">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-mono font-semibold text-amber-950">{item.notifyNo}</span>
                    <Badge tone="warning">Không sync</Badge>
                  </div>
                  <p className="mt-2 text-amber-900">{item.message}</p>
                  <p className="mt-2 break-all text-slate-500">
                    Folder mới: <span className="font-mono text-slate-700">{item.newPath}</span>
                  </p>
                  <p className="mt-1 break-all text-slate-500">
                    Đang tồn tại: <span className="font-mono text-slate-700">{item.existingPath || 'Chưa có đường dẫn lưu'}</span>
                  </p>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
