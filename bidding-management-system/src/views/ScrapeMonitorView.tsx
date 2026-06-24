import { useMemo, useState } from 'react';
import { Database, RefreshCw, Terminal } from 'lucide-react';
import { PackageData, ScrapingLog } from '../types';
import { Button } from '../components/ui/Button';
import { StatusBadge } from '../components/ui/StatusBadge';
import { formatDateTime } from '../utils/date';

interface ScrapeMonitorProps {
  packages: PackageData[];
  onTriggerSyncSingle: (notifyNo: string) => void;
  isSyncing: boolean;
  onSelectPackage: (pkg: PackageData) => void;
}

const ALL_PACKAGES = 'Tất cả';

export default function ScrapeMonitorView({
  packages,
  onTriggerSyncSingle,
  isSyncing,
  onSelectPackage
}: ScrapeMonitorProps) {
  const [selectedPackageForLogs, setSelectedPackageForLogs] = useState<string>(ALL_PACKAGES);
  const [severityFilter, setSeverityFilter] = useState<'ALL' | 'INFO' | 'WARN' | 'ERROR'>('ALL');

  const aggregatedLogs = useMemo(() => {
    let logs: (ScrapingLog & { notifyNo: string; title: string })[] = [];

    packages.forEach((pkg) => {
      if (selectedPackageForLogs === ALL_PACKAGES || pkg.notifyNo === selectedPackageForLogs) {
        logs = [
          ...logs,
          ...pkg.logs.map((log) => ({
            ...log,
            notifyNo: pkg.notifyNo,
            title: pkg.title
          }))
        ];
      }
    });

    logs.sort((a, b) => b.timestamp.localeCompare(a.timestamp));
    return severityFilter === 'ALL' ? logs : logs.filter((log) => log.level === severityFilter);
  }, [packages, selectedPackageForLogs, severityFilter]);

  return (
    <div className="space-y-4" id="view-scrape-monitor">
      <section className="rounded-lg border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-200 px-4 py-3">
          <h2 className="text-sm font-semibold text-slate-950">Trạng thái gói thầu trong scraper</h2>
        </div>

        {packages.length === 0 ? (
          <div className="px-4 py-10 text-center text-sm text-slate-500">Chưa có gói thầu để theo dõi.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-[980px] w-full border-separate border-spacing-0 text-left text-sm">
              <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
                <tr>
                  <th className="border-b border-slate-200 px-4 py-3">Gói thầu</th>
                  <th className="w-[180px] border-b border-slate-200 px-4 py-3">Trạng thái gói thầu</th>
                  <th className="w-[170px] border-b border-slate-200 px-4 py-3">Trạng thái scraper</th>
                  <th className="w-[180px] border-b border-slate-200 px-4 py-3">Lần đồng bộ cuối</th>
                  <th className="w-[120px] border-b border-slate-200 px-4 py-3 text-right">Đồng bộ</th>
                </tr>
              </thead>
              <tbody>
                {packages.map((pkg) => (
                  <tr
                    key={pkg.notifyNo}
                    onClick={() => onSelectPackage(pkg)}
                    className="cursor-pointer hover:bg-slate-50"
                  >
                    <td className="border-b border-slate-100 px-4 py-3">
                      <div className="max-w-[520px]">
                        <p className="font-mono text-xs font-medium text-slate-500">{pkg.notifyNo}</p>
                        <p className="mt-1 line-clamp-2 font-semibold text-slate-950">{pkg.title}</p>
                      </div>
                    </td>
                    <td className="border-b border-slate-100 px-4 py-3">
                      <StatusBadge status={pkg.lifecycle} />
                    </td>
                    <td className="border-b border-slate-100 px-4 py-3">
                      <StatusBadge status={pkg.scrapeStatus} />
                    </td>
                    <td className="border-b border-slate-100 px-4 py-3 font-mono text-xs text-slate-600">
                      {formatDateTime(pkg.lastSyncTime)}
                    </td>
                    <td className="border-b border-slate-100 px-4 py-3 text-right" onClick={(event) => event.stopPropagation()}>
                      <Button size="sm" onClick={() => onTriggerSyncSingle(pkg.notifyNo)} disabled={isSyncing}>
                        <RefreshCw className={`h-3.5 w-3.5 ${isSyncing ? 'animate-spin' : ''}`} />
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="flex flex-col space-y-4 rounded-lg border border-slate-800 bg-slate-950 p-4 font-mono text-slate-200 shadow-sm">
        <div className="flex flex-col justify-between gap-4 border-b border-slate-800 pb-4 md:flex-row md:items-center">
          <div className="flex items-center gap-3">
            <Terminal className="h-5 w-5 text-emerald-400" />
            <div>
              <h2 className="text-sm font-semibold text-slate-100">Nhật ký scraper</h2>
              <p className="mt-0.5 font-sans text-xs text-slate-400">Theo dõi tiến trình bóc tách và đồng bộ dữ liệu.</p>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2 font-sans text-xs">
            <label className="flex items-center gap-2 rounded-md border border-slate-700 bg-slate-900 px-2 py-1.5">
              <span className="text-slate-400">Gói thầu</span>
              <select
                value={selectedPackageForLogs}
                onChange={(event) => setSelectedPackageForLogs(event.target.value)}
                className="rounded border border-slate-700 bg-slate-950 px-2 py-1 text-xs text-slate-200 outline-none focus:border-slate-500"
              >
                <option value={ALL_PACKAGES}>Tất cả ({packages.length})</option>
                {packages.map((pkg) => (
                  <option key={pkg.notifyNo} value={pkg.notifyNo}>{pkg.notifyNo}</option>
                ))}
              </select>
            </label>

            <div className="flex items-center gap-1 rounded-md border border-slate-700 bg-slate-900 p-1">
              {(['ALL', 'INFO', 'WARN', 'ERROR'] as const).map((level) => (
                <button
                  key={level}
                  type="button"
                  onClick={() => setSeverityFilter(level)}
                  className={`rounded px-2 py-1 text-xs font-medium transition-colors ${
                    severityFilter === level ? 'bg-slate-700 text-white' : 'text-slate-400 hover:text-white'
                  }`}
                >
                  {level}
                </button>
              ))}
            </div>
          </div>
        </div>

        <div className="h-[420px] overflow-y-auto rounded-md border border-slate-800 bg-slate-950 p-3 text-xs leading-relaxed">
          {aggregatedLogs.length === 0 ? (
            <div className="flex h-full flex-col items-center justify-center text-center font-sans text-slate-500">
              <Database className="mb-2 h-8 w-8 text-slate-600" />
              <p className="text-sm font-medium text-slate-400">Không có nhật ký phù hợp bộ lọc</p>
              <p className="mt-1 text-xs">Sự kiện quét thầu sẽ hiển thị tại đây.</p>
            </div>
          ) : (
            <div className="space-y-1">
              {aggregatedLogs.map((log, index) => {
                const levelColor =
                  log.level === 'INFO'
                    ? 'text-emerald-400'
                    : log.level === 'WARN'
                      ? 'text-amber-400'
                      : 'text-rose-400';

                return (
                  <div key={`${log.notifyNo}-${log.timestamp}-${index}`} className="grid gap-2 rounded px-2 py-1 hover:bg-slate-900 md:grid-cols-[150px_58px_120px_minmax(0,1fr)]">
                    <span className="text-slate-500">{log.timestamp}</span>
                    <span className={`font-semibold ${levelColor}`}>[{log.level}]</span>
                    <span className="font-semibold text-sky-400">{log.notifyNo || 'CORE'}</span>
                    <span className="min-w-0 text-slate-300">
                      <span className="mr-1.5 text-slate-500">({log.stage})</span>
                      {log.message}
                    </span>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        <div className="flex flex-col justify-between gap-2 border-t border-slate-800 pt-3 font-sans text-xs text-slate-500 sm:flex-row sm:items-center">
          <p>Hiển thị <span className="font-semibold text-slate-300">{aggregatedLogs.length}</span> log</p>
          <div className="flex items-center gap-2">
            <span className="h-2 w-2 rounded-full bg-emerald-400" />
            <span>MSC gateway đang kết nối</span>
          </div>
        </div>
      </section>
    </div>
  );
}
