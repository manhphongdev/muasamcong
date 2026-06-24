import { ChevronLeft, ChevronRight } from 'lucide-react';

interface TrackingPaginationProps {
  page: number;
  pageSize: number;
  totalElements: number;
  setPage: (page: number) => void;
  setPageSize: (pageSize: number) => void;
  disabled?: boolean;
}

export function TrackingPagination({ page, pageSize, totalElements, setPage, setPageSize, disabled = false }: TrackingPaginationProps) {
  const safePageSize = Math.max(pageSize || 20, 1);
  const safeTotalElements = Math.max(totalElements || 0, 0);
  const totalPages = Math.ceil(safeTotalElements / safePageSize);
  const start = safeTotalElements > 0 ? page * safePageSize + 1 : 0;
  const end = Math.min(safeTotalElements, (page + 1) * safePageSize);

  const handlePageChange = (newPage: number) => {
    if (!disabled && newPage >= 0 && newPage < totalPages) {
      setPage(newPage);
    }
  };

  return (
    <div className="flex flex-col gap-3 border-t border-slate-200 bg-white px-4 py-3 text-xs text-slate-600 sm:flex-row sm:items-center sm:justify-between">
      <div className="flex flex-wrap items-center gap-3">
        <label className="flex items-center gap-2">
          <span>Hiển thị</span>
          <select
            value={safePageSize}
            disabled={disabled}
            onChange={(event) => {
              setPageSize(Number(event.target.value));
              setPage(0);
            }}
            className="rounded-md border border-slate-200 bg-white px-2 py-1.5 font-medium text-slate-900 outline-none focus:border-slate-400 focus:ring-2 focus:ring-slate-100 disabled:opacity-50"
          >
            <option value={20}>20</option>
            <option value={50}>50</option>
            <option value={100}>100</option>
          </select>
        </label>
        <span>
          {start}-{end} trong {safeTotalElements} gói thầu
        </span>
      </div>

      {totalPages > 1 && (
        <div className="flex items-center gap-1.5">
          <button
            onClick={() => handlePageChange(page - 1)}
            disabled={disabled || page === 0}
            className="rounded-md border border-slate-200 bg-white p-1.5 text-slate-600 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
            title="Trang trước"
          >
            <ChevronLeft className="h-4 w-4" />
          </button>

          <span className="px-2 font-medium text-slate-700">
            Trang {page + 1}/{totalPages}
          </span>

          <button
            onClick={() => handlePageChange(page + 1)}
            disabled={disabled || page === totalPages - 1}
            className="rounded-md border border-slate-200 bg-white p-1.5 text-slate-600 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
            title="Trang sau"
          >
            <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      )}
    </div>
  );
}
