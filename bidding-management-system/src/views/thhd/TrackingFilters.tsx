import { Search } from 'lucide-react';

interface TrackingFiltersProps {
  searchQuery: string;
  setSearchQuery: (query: string) => void;
  activeStatusFilter: string;
  setActiveStatusFilter: (status: string) => void;
  setPage: (page: number) => void;
}

const statusPills = [
  { id: 'ALL', label: 'Tất cả' },
  { id: 'Đang mời thầu', label: 'Mời thầu' },
  { id: 'Đã mở thầu', label: 'Mở thầu' },
  { id: 'Đã đóng thầu', label: 'Đã đóng' },
  { id: 'Có KQLCNT', label: 'Có KQLCNT' },
  { id: 'Có thông tin hợp đồng', label: 'Có hợp đồng' }
];

export function TrackingFilters({
  searchQuery,
  setSearchQuery,
  activeStatusFilter,
  setActiveStatusFilter,
  setPage
}: TrackingFiltersProps) {
  return (
    <section className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <div className="relative min-w-0">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
        <input
          type="text"
          value={searchQuery}
          onChange={(event) => {
            setSearchQuery(event.target.value);
            setPage(0);
          }}
          placeholder="Tìm theo số TBMT, tên gói thầu, chủ đầu tư..."
          className="h-10 w-full rounded-md border border-slate-200 bg-white pl-9 pr-3 text-sm text-slate-900 outline-none transition focus:border-slate-400 focus:ring-2 focus:ring-slate-100 placeholder:text-slate-400"
        />
      </div>

      <div className="mt-3 flex items-center gap-2 overflow-x-auto border-t border-slate-100 pt-3">
        <span className="shrink-0 text-xs font-medium text-slate-500">Trạng thái</span>
        {statusPills.map((pill) => (
          <button
            key={pill.id}
            onClick={() => {
              setActiveStatusFilter(pill.id);
              setPage(0);
            }}
            className={`shrink-0 rounded-md border px-2.5 py-1.5 text-xs font-medium transition-colors ${
              activeStatusFilter === pill.id
                ? 'border-slate-900 bg-slate-900 text-white'
                : 'border-slate-200 bg-white text-slate-600 hover:bg-slate-50'
            }`}
          >
            {pill.label}
          </button>
        ))}
      </div>
    </section>
  );
}
