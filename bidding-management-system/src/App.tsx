/**
 * @license
 * SPDX-License-Identifier: Apache-2.5
 */

import React, { useState, useMemo, useEffect } from 'react';
import Sidebar from './components/Sidebar';
import TenderDetailDrawer from './components/TenderDetailDrawer';
import { ToastItem, ToastTone, ToastViewport } from './components/ui/Toast';
import { mockPackages } from './mockPackages';
import { mockDownloadJobs } from './mockDownloadJobs';
import { mockMonitorRuns } from './mockMonitorRuns';
import {
  PackageData,
  DownloadJobItem,
  MonitorRun,
  ApiStageName,
  ScrapingLog,
  SyncPendingItemResult
} from './types';

// Import newly created views
import ImportCenterView from './views/ImportCenterView';
import ScrapeMonitorView from './views/ScrapeMonitorView';
import THHDTrackingView from './views/THHDTrackingView';

const TAB_ROUTES: Record<string, string> = {
  import_center: '/them-goi-thau',
  thhd_tracking: '/theo-doi-thhd',
  scrape_monitor: '/theo-doi-scraper'
};

const ROUTE_TABS: Record<string, string> = Object.entries(TAB_ROUTES).reduce(
  (routes, [tab, path]) => ({ ...routes, [path]: tab }),
  {} as Record<string, string>
);
const SIDEBAR_COLLAPSED_KEY = 'msc.sidebarCollapsed';

function tabFromPath(pathname: string): string {
  return ROUTE_TABS[pathname] || 'import_center';
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || `${window.location.protocol}//${window.location.hostname}:8888/api/v1`;

function numberParam(params: URLSearchParams, key: string, fallback: number, min = 0) {
  const value = Number(params.get(key));
  return Number.isFinite(value) && value >= min ? value : fallback;
}

function hasFileExtension(path: string) {
  const cleaned = path.trim().split(/[?#]/)[0];
  const lastSegment = cleaned.split(/[\\/]/).pop() || '';
  return /\.[A-Za-z0-9]{1,10}$/.test(lastSegment);
}

function encodeFileUrlPath(path: string) {
  return encodeURI(path).replace(/#/g, '%23');
}

function toBrowserFileUrl(rawPath: string) {
  const trimmed = rawPath.trim();
  if (/^file:\/\//i.test(trimmed)) return trimmed;

  const normalized = trimmed.replace(/\\/g, '/');
  if (/^\/\//.test(normalized)) {
    return `file://${encodeFileUrlPath(normalized.replace(/^\/+/, ''))}`;
  }

  if (/^[A-Za-z]:\//.test(normalized)) {
    return `file:///${encodeFileUrlPath(normalized)}`;
  }

  return encodeFileUrlPath(normalized);
}

export default function App() {
  const initialParams = new URLSearchParams(window.location.search);
  // Navigation Tabs Section routing
  const [activeTab, setActiveTab] = useState<string>(() => tabFromPath(window.location.pathname));
  const [sidebarCollapsed, setSidebarCollapsed] = useState<boolean>(() => localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === 'true');

  // Real-time active database lists
  const [packages, setPackages] = useState<PackageData[]>([]);
  const [downloads, setDownloads] = useState<DownloadJobItem[]>(mockDownloadJobs);
  const [runs, setRuns] = useState<MonitorRun[]>(mockMonitorRuns);
  const [isPackagesLoading, setIsPackagesLoading] = useState<boolean>(false);
  const [packagesError, setPackagesError] = useState<string | null>(null);

  // Pagination and filtering states
  const [page, setPage] = useState<number>(() => numberParam(initialParams, 'page', 0));
  const [pageSize, setPageSize] = useState<number>(() => numberParam(initialParams, 'size', 20, 1));
  const [totalElements, setTotalElements] = useState<number>(0);
  const [searchQuery, setSearchQuery] = useState<string>(() => initialParams.get('search') || '');
  const [debouncedSearchQuery, setDebouncedSearchQuery] = useState<string>(() => initialParams.get('search') || '');
  const [activeStatusFilter, setActiveStatusFilter] = useState<string>(() => {
    const status = initialParams.get('status') || 'ALL';
    return status === 'Đã hủy TBMT' ? 'ALL' : status;
  });
  const [activeKpiFilter, setActiveKpiFilter] = useState<string>(() => initialParams.get('kpiFilter') || 'ALL');

  // Detail Drawer state controllers
  const [selectedPackage, setSelectedPackage] = useState<PackageData | null>(null);
  const [isDrawerOpen, setIsDrawerOpen] = useState<boolean>(false);

  // Orchestrator status triggers
  const [isSyncing, setIsSyncing] = useState<boolean>(false);
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const showToast = (message: string, tone: ToastTone = 'info') => {
    const id = Date.now() + Math.floor(Math.random() * 1000);
    setToasts(prev => [...prev, { id, message, tone }].slice(-4));
    window.setTimeout(() => {
      setToasts(prev => prev.filter(toast => toast.id !== id));
    }, 4200);
  };

  useEffect(() => {
    const originalAlert = window.alert;
    window.alert = (message?: unknown) => {
      const text = String(message ?? '');
      const lowerText = text.toLowerCase();
      const isError = lowerText.includes('lỗi') || lowerText.includes('thất bại') || lowerText.includes('không thể');
      showToast(text, isError ? 'error' : 'success');
    };
    return () => {
      window.alert = originalAlert;
    };
  }, []);

  const normalizePackage = (pkg: PackageData): PackageData => ({
    ...pkg,
    lifecycle: ((pkg.lifecycle as string) === 'Đã hủy TBMT' ? 'Không xác định' : pkg.lifecycle) as PackageData['lifecycle']
  });

  const fetchPackages = async () => {
    setIsPackagesLoading(true);
    setPackagesError(null);
    try {
      const searchParam = debouncedSearchQuery.trim() ? `&search=${encodeURIComponent(debouncedSearchQuery.trim())}` : '';
      const statusParam = activeStatusFilter !== 'ALL' ? `&status=${encodeURIComponent(activeStatusFilter)}` : '';
      const kpiParam = activeKpiFilter !== 'ALL' ? `&kpiFilter=${encodeURIComponent(activeKpiFilter)}` : '';
      
      const response = await fetch(
        `${API_BASE_URL}/bid-packages/tracking?page=${page}&size=${pageSize}${searchParam}${statusParam}${kpiParam}`
      );
      if (response.ok) {
        const result = await response.json();
        if (result.success && result.data) {
          const content = (result.data.content || []).map(normalizePackage);
          setPackages(content);
          setTotalElements(result.data.totalElements || 0);
          return content;
        }
        throw new Error(result.message || 'Dữ liệu trả về không hợp lệ');
      }
      throw new Error(`HTTP ${response.status}`);
    } catch (error) {
      console.error("Error fetching packages:", error);
      setPackagesError(error instanceof Error ? error.message : 'Không thể tải danh sách gói thầu');
    } finally {
      setIsPackagesLoading(false);
    }
    return null;
  };

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setDebouncedSearchQuery(searchQuery);
    }, 400);
    return () => window.clearTimeout(timer);
  }, [searchQuery]);

  useEffect(() => {
    fetchPackages();
  }, [page, pageSize, debouncedSearchQuery, activeStatusFilter, activeKpiFilter]);

  useEffect(() => {
    if (activeTab !== 'thhd_tracking') return;
    const params = new URLSearchParams(window.location.search);
    params.set('page', String(page));
    params.set('size', String(pageSize));
    if (searchQuery.trim()) {
      params.set('search', searchQuery.trim());
    } else {
      params.delete('search');
    }
    if (activeStatusFilter !== 'ALL') {
      params.set('status', activeStatusFilter);
    } else {
      params.delete('status');
    }
    if (activeKpiFilter !== 'ALL') {
      params.set('kpiFilter', activeKpiFilter);
    } else {
      params.delete('kpiFilter');
    }
    const searchStr = params.toString() ? `?${params.toString()}` : '';
    window.history.replaceState(null, '', `${window.location.pathname}${searchStr}`);
  }, [activeTab, page, pageSize, searchQuery, activeStatusFilter, activeKpiFilter]);

  // Simulated live downloads progress ticker
  useEffect(() => {
    let timer: NodeJS.Timeout;
    if (isSyncing) {
      timer = setInterval(() => {
        setDownloads(prevDls => {
          let hasPending = false;
          const updated = prevDls.map(dl => {
            if (dl.status === 'DOWNLOADING') {
              hasPending = true;
              const nextProgress = dl.progressPercent + Math.floor(Math.random() * 15 + 10);
              if (nextProgress >= 100) {
                return {
                  ...dl,
                  progressPercent: 100,
                  status: 'DOWNLOADED' as const,
                  lastError: null
                };
              }
              return {
                ...dl,
                progressPercent: nextProgress
              };
            }
            return dl;
          });
          
          if (!hasPending) {
            // Check if there are WAITING files to start downloading
            const waitingIdx = updated.findIndex(dl => dl.status === 'WAITING');
            if (waitingIdx !== -1) {
              updated[waitingIdx] = {
                ...updated[waitingIdx],
                status: 'DOWNLOADING' as const,
                progressPercent: 15
              };
            }
          }
          return updated;
        });
      }, 1500);
    }
    return () => clearInterval(timer);
  }, [isSyncing]);

  // Sync single package
  const handleTriggerSyncSingle = async (notifyNo: string) => {
    setIsSyncing(true);
    try {
      const res = await fetch(`${API_BASE_URL}/bid-packages/sync/${notifyNo}`, { method: 'POST' });
      const body = await res.json() as { success: boolean; message: string; data: SyncPendingItemResult };
      if (!res.ok || !body.success || !body.data?.success) {
        alert(body.data?.message || body.message || `Loi dong bo goi thau ${notifyNo}`);
        return;
      }
      if (res.ok && body.success && body.data?.success) {
        alert(`Đã hoàn tất đồng bộ dữ liệu cho gói thầu ${notifyNo}!`);
        const updatedList = await fetchPackages();
        if (updatedList) {
          const updatedPkg = updatedList.find(p => p.notifyNo === notifyNo);
          if (updatedPkg) {
            setSelectedPackage(updatedPkg);
          }
        }
      } else {
        alert(`Lỗi đồng bộ gói thầu ${notifyNo}`);
      }
    } catch (err) {
      console.error(err);
      alert("Lỗi kết nối server đồng bộ!");
    } finally {
      setIsSyncing(false);
    }
  };

  // Stop single or all processes
  const handleStopAllProcesses = () => {
    setIsSyncing(false);
    setPackages(prev => 
      prev.map(p => p.scrapeStatus !== 'COMPLETED' && p.scrapeStatus !== 'FAILED' ? { ...p, scrapeStatus: 'PARTIAL' } : p)
    );
    alert("Bộ ngắt khẩn cấp: Đã gỡ dừng toàn bộ tiến trình Scrape-crawler hằng ngày.");
  };

  // Purge/clear error queue
  const handleClearQueue = () => {
    setDownloads(prev => 
      prev.map(dl => dl.status === 'FAILED' ? { ...dl, status: 'SKIPPED_EXISTING', lastError: null } : dl)
    );
    alert("Đã gỡ lọc dọn dẹp hàng đợi. Đánh dấu các tệp lỗi hỏng thành SKIPPED để bảo toàn đường chạy.");
  };

  // Trigger individual download retry
  const handleRetryDownload = (id: string) => {
    setDownloads(prev => 
      prev.map(dl => {
        if (dl.id === id) {
          return {
            ...dl,
            status: 'DOWNLOADING',
            progressPercent: 20,
            lastError: null
          };
        }
        return dl;
      })
    );
  };

  // Bulk retry failed files
  const handleBulkRetryDownloads = () => {
    setDownloads(prev => 
      prev.map(dl => dl.status === 'FAILED' ? { ...dl, status: 'DOWNLOADING', progressPercent: 10, lastError: null } : dl)
    );
    alert("Đang khởi động kết nối tải lại hàng loạt các tệp tin hỏng lỗi...");
  };

  // Update folder inline
  const handleUpdateFolderInline = async (notifyNo: string, folder: string | null) => {
    try {
      const res = await fetch(`${API_BASE_URL}/bid-packages/update-folder?notifyNo=${notifyNo}&folderPath=${encodeURIComponent(folder || '')}`, {
        method: 'POST'
      });
      if (res.ok) {
        const updatedList = await fetchPackages();
        if (updatedList) {
          const updatedPkg = updatedList.find(p => p.notifyNo === notifyNo);
          if (updatedPkg) {
            setSelectedPackage(updatedPkg);
          }
        }
      } else {
        alert("Không thể cập nhật đường dẫn thư mục!");
      }
    } catch (err) {
      console.error(err);
      alert("Lỗi kết nối máy chủ!");
    }
  };

  const handleOpenFolder = (folderPath: string) => {
    if (!folderPath?.trim()) {
      alert('Chưa có đường dẫn thư mục.');
      return;
    }
    const fileUrl = toBrowserFileUrl(folderPath);
    const opened = window.open(fileUrl, '_blank', 'noopener,noreferrer');
    if (!opened) {
      showToast('Browser đã chặn mở file. Hãy dùng nút Sao chép.', 'error');
    }
  };

  const handleCopyFolder = async (folderPath: string) => {
    if (!folderPath?.trim()) {
      alert('Chưa có đường dẫn thư mục.');
      return;
    }
    const fileUrl = toBrowserFileUrl(folderPath);
    try {
      await navigator.clipboard.writeText(fileUrl);
      alert(hasFileExtension(folderPath) ? 'Đã sao chép đường dẫn file.' : 'Đã sao chép đường dẫn thư mục.');
    } catch (err) {
      console.error(err);
      alert(fileUrl);
    }
  };

  // Terminate/delete package from queue
  const handleDeletePackage = (notifyNo: string) => {
    setPackages(prev => prev.filter(p => p.notifyNo !== notifyNo));
  };

  // Sidebar navigation shortcut action
  const handleNavigateToTab = (tabId: string) => {
    const nextPath = TAB_ROUTES[tabId] || TAB_ROUTES.import_center;
    if (window.location.pathname !== nextPath) {
      window.history.pushState(null, '', nextPath);
    }
    setActiveTab(tabId);
  };

  useEffect(() => {
    const handlePopState = () => {
      setActiveTab(tabFromPath(window.location.pathname));
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  useEffect(() => {
    localStorage.setItem(SIDEBAR_COLLAPSED_KEY, String(sidebarCollapsed));
  }, [sidebarCollapsed]);

  // Handle auto-opening detail drawer from notifyNo URL query parameter
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const notifyNo = params.get('notifyNo');
    if (notifyNo) {
      const pkg = packages.find(p => p.notifyNo === notifyNo);
      if (pkg) {
        setSelectedPackage(pkg);
        setIsDrawerOpen(true);
      }
    }
  }, [packages]);

  // Load selected package into detail drawer
  const handleSelectPackage = (pkg: PackageData) => {
    setSelectedPackage(pkg);
    setIsDrawerOpen(true);
    // Sync URL query parameters
    const params = new URLSearchParams(window.location.search);
    params.set('notifyNo', pkg.notifyNo);
    window.history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
  };

  // Counts for the layout badges
  const queuedCount = useMemo(() => {
    return packages.length;
  }, [packages]);

  const activeDownloadsCount = useMemo(() => {
    return downloads.filter(d => d.status === 'DOWNLOADING' || d.status === 'FAILED').length;
  }, [downloads]);

  return (
    <div className="bg-slate-50 min-h-screen font-sans flex antialiased" id="scraper-workspace">
      
      {/* 1. Left Drawer Navigation Sidebar */}
      <Sidebar 
        activeSection={activeTab} 
        setActiveSection={handleNavigateToTab}
        packagesCount={queuedCount}
        downloadCount={activeDownloadsCount}
        collapsed={sidebarCollapsed}
        onToggleCollapsed={() => setSidebarCollapsed(prev => !prev)}
        disabled={isDrawerOpen}
      />

      {/* 2. Main content canvas board */}
      <div className={`${sidebarCollapsed ? 'ml-[76px]' : 'ml-[248px]'} flex-1 min-w-0 overflow-x-hidden flex flex-col min-h-screen transition-all duration-200`}>
        
        {/* Dynamic header row metadata */}
        <header className="bg-white border-b border-slate-200 px-6 py-4 flex items-center justify-between sticky top-0 z-40" id="workspace-header">
          <div>
            <h2 className="text-lg font-semibold font-sans text-slate-950 mt-1">
              {activeTab === 'import_center' && 'Nhập gói thầu'}
              {activeTab === 'thhd_tracking' && 'Theo dõi gói thầu'}
              {activeTab === 'scrape_monitor' && 'Theo dõi đồng bộ'}
            </h2>
          </div>
        </header>

        {/* 3. Render dynamically based on Tab Selection active tab */}
        <main className="p-4 sm:p-5 xl:p-6 w-full max-w-none min-w-0 overflow-x-hidden space-y-5 flex-grow pb-20">
          
          {activeTab === 'import_center' && (
            <ImportCenterView />
          )}

          {activeTab === 'thhd_tracking' && (
            <THHDTrackingView
              packages={packages}
              onUpdateFolder={handleUpdateFolderInline}
              onSelectPackage={handleSelectPackage}
              onTriggerSyncSingle={handleTriggerSyncSingle}
              isSyncing={isSyncing}
              page={page}
              setPage={setPage}
              pageSize={pageSize}
              setPageSize={setPageSize}
              totalElements={totalElements}
              searchQuery={searchQuery}
              setSearchQuery={setSearchQuery}
              activeStatusFilter={activeStatusFilter}
              setActiveStatusFilter={setActiveStatusFilter}
              activeKpiFilter={activeKpiFilter}
              setActiveKpiFilter={setActiveKpiFilter}
              onOpenFolder={handleOpenFolder}
              onCopyFolder={handleCopyFolder}
              isLoading={isPackagesLoading}
              error={packagesError}
              onRetry={fetchPackages}
            />
          )}

          {activeTab === 'scrape_monitor' && (
            <ScrapeMonitorView
              packages={packages}
              onTriggerSyncSingle={handleTriggerSyncSingle}
              isSyncing={isSyncing}
              onSelectPackage={handleSelectPackage}
            />
          )}

        </main>

      </div>

      {/* Right details drawer panel view details */}
      <TenderDetailDrawer
        pkg={selectedPackage}
        isOpen={isDrawerOpen}
        onClose={() => {
          setIsDrawerOpen(false);
          setSelectedPackage(null);
          // Clean up URL query parameters on close
          const params = new URLSearchParams(window.location.search);
          params.delete('notifyNo');
          const searchStr = params.toString() ? `?${params.toString()}` : '';
          window.history.replaceState(null, '', `${window.location.pathname}${searchStr}`);
        }}
        onTriggerSyncSingle={handleTriggerSyncSingle}
        isSyncing={isSyncing}
        apiBaseUrl={API_BASE_URL}
        onOpenFolder={handleOpenFolder}
        onCopyFolder={handleCopyFolder}
      />

      <ToastViewport toasts={toasts} onDismiss={(id) => setToasts(prev => prev.filter(toast => toast.id !== id))} />

    </div>
  );
}
