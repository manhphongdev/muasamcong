import { useEffect, useMemo, useState } from 'react';
import {
  AlertTriangle,
  Check,
  Clipboard,
  Clock3,
  FileText,
  FolderOpen,
  RefreshCw,
  SlidersHorizontal
} from 'lucide-react';
import { PackageData } from '../types';
import { Button } from '../components/ui/Button';
import { TableSkeleton } from '../components/ui/Skeleton';
import { StatusBadge } from '../components/ui/StatusBadge';
import { MetricCard } from '../components/ui/MetricCard';
import { TrackingFilters } from './thhd/TrackingFilters';
import { TrackingPagination } from './thhd/TrackingPagination';
import { formatDateTime, getTimeMs } from '../utils/date';

interface THHDTrackingViewProps {
  packages: PackageData[];
  onUpdateFolder: (notifyNo: string, folder: string | null) => void;
  onSelectPackage: (pkg: PackageData) => void;
  onTriggerSyncSingle: (notifyNo: string) => void;
  isSyncing: boolean;
  page: number;
  setPage: (page: number) => void;
  pageSize: number;
  setPageSize: (pageSize: number) => void;
  totalElements: number;
  searchQuery: string;
  setSearchQuery: (query: string) => void;
  activeStatusFilter: string;
  setActiveStatusFilter: (status: string) => void;
  activeKpiFilter: string;
  setActiveKpiFilter: (kpi: string) => void;
  onOpenFolder: (folderPath: string) => void;
  onCopyFolder: (folderPath: string) => void;
  isLoading: boolean;
  error: string | null;
  onRetry: () => void;
}

function formatMoney(value?: number) {
  if (!value) return 'Chưa có';
  return `${value.toLocaleString('vi-VN')} VND`;
}

function getRemaining(closeTime: string | null, now: number) {
  const closeMs = getTimeMs(closeTime);
  if (closeMs == null) return { label: 'Chưa có hạn', tone: 'neutral' as const };

  const diffMs = closeMs - now;
  if (diffMs <= 0) return { label: 'Đã đóng', tone: 'closed' as const };

  const totalMinutes = Math.floor(diffMs / 60000);
  const hours = Math.floor(totalMinutes / 60);
  const days = Math.floor(hours / 24);
  const hourPart = hours % 24;
  const minutePart = totalMinutes % 60;
  const label = days > 0 ? `${days} ngày ${hourPart} giờ` : `${Math.max(hours, 0)} giờ ${minutePart} phút`;
  const tone = hours <= 4 ? 'danger' : hours <= 24 ? 'warning' : 'info';
  return { label, tone };
}

function getExecutionTime(pkg: PackageData) {
  if (pkg.winningContractor) {
    const winner = pkg.bidders?.find((bidder) => bidder.name === pkg.winningContractor);
    if (winner?.executionTimeText) return winner.executionTimeText;
  }
  return pkg.bidders?.[0]?.executionTimeText || 'Chưa có';
}

const countdownToneClass = {
  neutral: 'bg-slate-100 text-slate-600 border-slate-200',
  closed: 'bg-slate-800 text-white border-slate-800',
  info: 'bg-sky-50 text-sky-700 border-sky-200',
  warning: 'bg-amber-50 text-amber-700 border-amber-200',
  danger: 'bg-rose-50 text-rose-700 border-rose-200'
};

const COLUMN_VISIBILITY_KEY = 'msc.thhd.visibleColumns';

const configurableColumns = [
  { id: 'budget', label: 'Dự toán' },
  { id: 'winner', label: 'Nhà thầu trúng thầu' },
  { id: 'publishDate', label: 'Ngày đăng tải' },
  { id: 'executionTime', label: 'Thời gian thực hiện' },
  { id: 'closeTime', label: 'Đóng thầu' },
  { id: 'remaining', label: 'Còn lại' },
  { id: 'status', label: 'Trạng thái' },
  { id: 'folder', label: 'Thư mục THHD' }
] as const;

type ConfigurableColumnId = typeof configurableColumns[number]['id'];
type ColumnVisibility = Record<ConfigurableColumnId, boolean>;

const defaultVisibleColumns: ColumnVisibility = configurableColumns.reduce((columns, column) => {
  columns[column.id] = true;
  return columns;
}, {} as ColumnVisibility);

function loadVisibleColumns(): ColumnVisibility {
  try {
    const raw = window.localStorage.getItem(COLUMN_VISIBILITY_KEY);
    if (!raw) return defaultVisibleColumns;
    const parsed = JSON.parse(raw) as Partial<ColumnVisibility>;
    return configurableColumns.reduce((columns, column) => {
      columns[column.id] = parsed[column.id] ?? true;
      return columns;
    }, {} as ColumnVisibility);
  } catch {
    return defaultVisibleColumns;
  }
}

function getOpenPathLabel(path: string) {
  const cleaned = path.trim().split(/[?#]/)[0];
  const lastSegment = cleaned.split(/[\\/]/).pop() || '';
  return /\.[A-Za-z0-9]{1,10}$/.test(lastSegment) ? 'Mở file' : 'Mở thư mục';
}

export default function THHDTrackingView({
  packages,
  onSelectPackage,
  page,
  setPage,
  pageSize,
  setPageSize,
  totalElements,
  searchQuery,
  setSearchQuery,
  activeStatusFilter,
  setActiveStatusFilter,
  activeKpiFilter,
  setActiveKpiFilter,
  onOpenFolder,
  onCopyFolder,
  isLoading,
  error,
  onRetry
}: THHDTrackingViewProps) {
  const [now, setNow] = useState(Date.now());
  const [copied, setCopied] = useState<string | null>(null);
  const [visibleColumns, setVisibleColumns] = useState<ColumnVisibility>(() => loadVisibleColumns());
  const [isColumnMenuOpen, setIsColumnMenuOpen] = useState(false);

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 30000);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    window.localStorage.setItem(COLUMN_VISIBILITY_KEY, JSON.stringify(visibleColumns));
  }, [visibleColumns]);

  const closing24h = useMemo(() => {
    return packages.filter((pkg) => {
      if (pkg.lifecycle !== 'Đang mời thầu') return false;
      const closeMs = getTimeMs(pkg.closeTime);
      if (closeMs == null) return false;
      const hours = (closeMs - now) / 3600000;
      return hours > 0 && hours <= 24;
    }).length;
  }, [packages, now]);

  const copyText = async (value: string) => {
    await navigator.clipboard.writeText(value);
    setCopied(value);
    window.setTimeout(() => setCopied(null), 1800);
  };

  const toggleColumn = (columnId: ConfigurableColumnId) => {
    setVisibleColumns((current) => ({
      ...current,
      [columnId]: !current[columnId]
    }));
  };

  return (
    <div className="space-y-4 min-w-0" id="view-thhd-tracking">
      <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
        <MetricCard
          label="Tổng gói thầu"
          value={totalElements || packages.length}
          icon={<FileText className="h-5 w-5" />}
          active={activeKpiFilter === 'ALL'}
          onClick={() => {
            setActiveKpiFilter('ALL');
            setPage(0);
          }}
        />
        <MetricCard
          label="Sắp đóng 24h"
          value={closing24h}
          icon={<Clock3 className="h-5 w-5" />}
          tone="warning"
          active={activeKpiFilter === 'SHIFT_24H'}
          onClick={() => {
            setActiveKpiFilter('SHIFT_24H');
            setPage(0);
          }}
        />
      </div>

      <TrackingFilters
        searchQuery={searchQuery}
        setSearchQuery={setSearchQuery}
        activeStatusFilter={activeStatusFilter}
        setActiveStatusFilter={setActiveStatusFilter}
        setPage={setPage}
      />

      <section className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-200 px-4 py-3">
          <div className="flex flex-col gap-3 xl:flex-row xl:items-start xl:justify-between">
            <div>
              <h3 className="text-sm font-semibold text-slate-950">Danh sách theo dõi THHD</h3>
            </div>
            <div className="relative">
              <button
                type="button"
                onClick={() => setIsColumnMenuOpen((open) => !open)}
                className="inline-flex items-center gap-2 rounded-md border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50"
              >
                <SlidersHorizontal className="h-3.5 w-3.5" />
                Cột hiển thị
              </button>
              {isColumnMenuOpen && (
                <div className="absolute right-0 top-10 z-40 w-64 rounded-lg border border-slate-200 bg-white p-2 shadow-lg">
                  <div className="border-b border-slate-100 px-2 pb-2 text-xs font-medium text-slate-500">
                    Chọn cột hiển thị
                  </div>
                  <div className="mt-2 space-y-1">
                    {configurableColumns.map((column) => (
                      <label
                        key={column.id}
                        className="flex cursor-pointer items-center gap-2 rounded-md px-2 py-1.5 text-sm text-slate-700 hover:bg-slate-50"
                      >
                        <input
                          type="checkbox"
                          checked={visibleColumns[column.id]}
                          onChange={() => toggleColumn(column.id)}
                          className="h-4 w-4 rounded border-slate-300 text-slate-900 focus:ring-slate-300"
                        />
                        {column.label}
                      </label>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>

        {isLoading ? (
          <TableSkeleton rows={8} columns={8} />
        ) : error ? (
          <div className="flex flex-col items-center justify-center gap-3 px-6 py-16 text-center">
            <AlertTriangle className="h-10 w-10 text-rose-400" />
            <div>
              <p className="text-sm font-semibold text-slate-800">Không thể tải danh sách gói thầu</p>
              <p className="mt-1 text-xs text-slate-500">{error}</p>
            </div>
            <Button variant="primary" onClick={onRetry}>
              <RefreshCw className="h-3.5 w-3.5" />
              Tải lại
            </Button>
          </div>
        ) : packages.length === 0 ? (
          <div className="flex flex-col items-center justify-center gap-2 px-6 py-16 text-center">
            <FileText className="h-10 w-10 text-slate-300" />
            <p className="text-sm font-semibold text-slate-700">Không có gói thầu phù hợp</p>
            <p className="text-xs text-slate-500">Thử đổi từ khóa hoặc bỏ bớt bộ lọc trạng thái.</p>
          </div>
        ) : (
          <>
            <div className="hidden overflow-x-auto md:block">
              <table className="min-w-[1928px] w-full border-separate border-spacing-0 text-left text-sm">
                <thead>
                  <tr className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
                    <th className="sticky left-0 z-30 w-[520px] min-w-[520px] border-b border-r border-slate-200 bg-slate-50 px-4 py-3">Gói thầu</th>
                    {visibleColumns.budget && <th className="w-[210px] border-b border-slate-200 px-4 py-3">Dự toán</th>}
                    {visibleColumns.winner && <th className="w-[380px] border-b border-slate-200 px-4 py-3">Nhà thầu trúng thầu</th>}
                    {visibleColumns.publishDate && <th className="w-[130px] border-b border-slate-200 px-4 py-3">Ngày đăng tải</th>}
                    {visibleColumns.executionTime && <th className="w-[170px] border-b border-slate-200 px-4 py-3">Thời gian thực hiện</th>}
                    {visibleColumns.closeTime && <th className="w-[175px] border-b border-slate-200 px-4 py-3">Đóng thầu</th>}
                    {visibleColumns.remaining && <th className="w-[150px] border-b border-slate-200 px-4 py-3">Còn lại</th>}
                    {visibleColumns.status && <th className="w-[170px] border-b border-slate-200 px-4 py-3">Trạng thái</th>}
                    {visibleColumns.folder && <th className="w-[260px] border-b border-slate-200 px-4 py-3">Thư mục THHD</th>}
                  </tr>
                </thead>
                <tbody>
                  {packages.map((pkg) => {
                    const remaining = getRemaining(pkg.closeTime, now);
                    const hasFolder = Boolean(pkg.folderPath && pkg.folderExists);
                    return (
                      <tr
                        key={pkg.notifyNo}
                        className="group cursor-pointer border-b border-slate-100 hover:bg-slate-50/80"
                        onClick={() => onSelectPackage(pkg)}
                      >
                        <td className="sticky left-0 z-20 border-b border-r border-slate-100 bg-white px-4 py-3 group-hover:bg-slate-50">
                          <div className="max-w-[490px]">
                            <button
                              type="button"
                              onClick={(event) => {
                                event.stopPropagation();
                                copyText(pkg.notifyNo);
                              }}
                              className="mb-1 inline-flex items-center gap-1.5 rounded-md bg-slate-100 px-2 py-0.5 font-mono text-[11px] font-medium text-slate-600 hover:bg-slate-200"
                              title="Sao chép số TBMT"
                            >
                              {pkg.notifyNo}
                              {copied === pkg.notifyNo ? <Check className="h-3 w-3 text-emerald-600" /> : <Clipboard className="h-3 w-3 text-slate-400" />}
                            </button>
                            <p className="line-clamp-2 text-sm font-semibold leading-5 text-slate-950">{pkg.title}</p>
                            <p className="mt-1 truncate text-xs text-slate-500">{pkg.investor}</p>
                          </div>
                        </td>
                        {visibleColumns.budget && (
                          <td className="border-b border-slate-100 px-4 py-3">
                            <p className="font-medium text-slate-900">{formatMoney(pkg.budget)}</p>
                            {pkg.winningPrice ? <p className="mt-0.5 text-xs text-emerald-700">Trúng: {formatMoney(pkg.winningPrice)}</p> : null}
                          </td>
                        )}
                        {visibleColumns.winner && (
                          <td className="border-b border-slate-100 px-4 py-3">
                            {pkg.winningContractor ? (
                              <p className="line-clamp-3 max-w-[360px] text-sm font-medium leading-5 text-slate-900">{pkg.winningContractor}</p>
                            ) : (
                              <span className="text-xs text-slate-400">Chưa công bố</span>
                            )}
                          </td>
                        )}
                        {visibleColumns.publishDate && (
                          <td className="border-b border-slate-100 px-4 py-3 font-mono text-xs text-slate-600">
                            {pkg.publishDate || '--'}
                          </td>
                        )}
                        {visibleColumns.executionTime && (
                          <td className="border-b border-slate-100 px-4 py-3 text-sm text-slate-700">
                            {getExecutionTime(pkg)}
                          </td>
                        )}
                        {visibleColumns.closeTime && (
                          <td className="border-b border-slate-100 px-4 py-3 font-mono text-xs text-slate-600">
                            {formatDateTime(pkg.closeTime)}
                          </td>
                        )}
                        {visibleColumns.remaining && (
                          <td className="border-b border-slate-100 px-4 py-3">
                            <span className={`inline-flex rounded-md border px-2 py-1 text-xs font-semibold ${countdownToneClass[remaining.tone]}`}>
                              {remaining.label}
                            </span>
                          </td>
                        )}
                        {visibleColumns.status && (
                          <td className="border-b border-slate-100 px-4 py-3">
                            <StatusBadge status={pkg.lifecycle} />
                          </td>
                        )}
                        {visibleColumns.folder && (
                          <td className="border-b border-slate-100 px-4 py-3" onClick={(event) => event.stopPropagation()}>
                            {hasFolder ? (
                            <div className="flex items-center gap-1.5">
                              <button
                                type="button"
                                onClick={() => onOpenFolder(pkg.folderPath!)}
                                className="inline-flex items-center gap-1.5 rounded-md border border-emerald-200 bg-emerald-50 px-2 py-1 text-xs font-semibold text-emerald-700 hover:bg-emerald-100"
                              >
                                <FolderOpen className="h-3.5 w-3.5 shrink-0" />
                                {getOpenPathLabel(pkg.folderPath!)}
                              </button>
                              <button
                                type="button"
                                onClick={() => onCopyFolder(pkg.folderPath!)}
                                className="rounded-md border border-slate-200 bg-white p-1.5 text-slate-500 hover:bg-slate-50"
                                title="Sao chép đường dẫn"
                              >
                                <Clipboard className="h-3.5 w-3.5" />
                              </button>
                            </div>
                            ) : (
                              <span className="text-xs text-slate-400">Chưa có</span>
                            )}
                          </td>
                        )}
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            <div className="divide-y divide-slate-100 md:hidden">
              {packages.map((pkg) => {
                const remaining = getRemaining(pkg.closeTime, now);
                const hasFolder = Boolean(pkg.folderPath && pkg.folderExists);
                return (
                  <article key={pkg.notifyNo} className="space-y-3 p-4" onClick={() => onSelectPackage(pkg)}>
                    <div className="min-w-0">
                      <h4 className="mt-1 line-clamp-2 text-sm font-semibold leading-5 text-slate-950">{pkg.title}</h4>
                      <p className="mt-1 truncate text-xs text-slate-500">
                        <span className="font-mono">{pkg.notifyNo}</span>
                        <span className="mx-1.5 text-slate-300">/</span>
                        {pkg.investor}
                      </p>
                    </div>
                    <div className="grid grid-cols-2 gap-2 text-xs">
                      {visibleColumns.budget && (
                        <div className="rounded-md bg-slate-50 p-2">
                          <p className="text-slate-500">Dự toán</p>
                          <p className="mt-1 font-semibold text-slate-900">{formatMoney(pkg.budget)}</p>
                        </div>
                      )}
                      {visibleColumns.publishDate && (
                        <div className="rounded-md bg-slate-50 p-2">
                          <p className="text-slate-500">Ngày đăng tải</p>
                          <p className="mt-1 font-mono font-semibold text-slate-900">{pkg.publishDate || '--'}</p>
                        </div>
                      )}
                      {visibleColumns.executionTime && (
                        <div className="rounded-md bg-slate-50 p-2">
                          <p className="text-slate-500">Thời gian thực hiện</p>
                          <p className="mt-1 font-semibold text-slate-900">{getExecutionTime(pkg)}</p>
                        </div>
                      )}
                      {visibleColumns.closeTime && (
                        <div className="rounded-md bg-slate-50 p-2">
                          <p className="text-slate-500">Đóng thầu</p>
                          <p className="mt-1 font-mono font-semibold text-slate-900">{formatDateTime(pkg.closeTime)}</p>
                        </div>
                      )}
                      {visibleColumns.remaining && (
                        <div className="rounded-md bg-slate-50 p-2">
                          <p className="text-slate-500">Còn lại</p>
                          <p className={`mt-1 inline-flex rounded-md border px-1.5 py-0.5 font-semibold ${countdownToneClass[remaining.tone]}`}>{remaining.label}</p>
                        </div>
                      )}
                    </div>
                    {visibleColumns.winner && (
                      <div className="rounded-md bg-slate-50 p-2 text-xs">
                        <p className="text-slate-500">Nhà thầu trúng thầu</p>
                        <p className="mt-1 line-clamp-2 font-semibold text-slate-900">{pkg.winningContractor || 'Chưa công bố'}</p>
                      </div>
                    )}
                    {visibleColumns.status && (
                      <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
                        <StatusBadge status={pkg.lifecycle} />
                      </div>
                    )}
                    {visibleColumns.folder && (
                      <div className="border-t border-slate-100 pt-3" onClick={(event) => event.stopPropagation()}>
                        {hasFolder ? (
                        <div className="flex items-center gap-1.5">
                          <button
                            type="button"
                            onClick={() => onOpenFolder(pkg.folderPath!)}
                            className="inline-flex items-center gap-1.5 rounded-md border border-emerald-200 bg-emerald-50 px-2 py-1 text-xs font-semibold text-emerald-700"
                          >
                            <FolderOpen className="h-3.5 w-3.5 shrink-0" />
                            {getOpenPathLabel(pkg.folderPath!)}
                          </button>
                          <button
                            type="button"
                            onClick={() => onCopyFolder(pkg.folderPath!)}
                            className="rounded-md border border-slate-200 bg-white p-1.5 text-slate-500"
                            title="Sao chép đường dẫn"
                          >
                            <Clipboard className="h-3.5 w-3.5" />
                          </button>
                        </div>
                        ) : (
                          <span className="text-xs text-slate-400">Chưa có thư mục THHD</span>
                        )}
                      </div>
                    )}
                  </article>
                );
              })}
            </div>

            <TrackingPagination
              page={page}
              pageSize={pageSize}
              totalElements={totalElements}
              setPage={setPage}
              setPageSize={setPageSize}
              disabled={isLoading}
            />
          </>
        )}
      </section>
    </div>
  );
}
