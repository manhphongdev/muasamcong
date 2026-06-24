import { PackageLifecycle, ScrapeStatus } from '../../types';
import { Badge } from './Badge';

interface StatusBadgeProps {
  status: PackageLifecycle | ScrapeStatus | string;
  className?: string;
}

export function StatusBadge({ status, className = '' }: StatusBadgeProps) {
  const tone = (() => {
    if (status === 'Có KQLCNT' || status === 'Có thông tin hợp đồng' || status === 'COMPLETED') return 'success';
    if (status === 'Đang mời thầu' || status === 'PROCESSING' || status === 'FETCHING_TBMT') return 'info';
    if (status === 'Đã mở thầu' || status === 'Đã đóng thầu' || status === 'PARTIAL') return 'warning';
    if (status === 'FAILED') return 'danger';
    return 'neutral';
  })();

  return <Badge tone={tone} className={className}>{status}</Badge>;
}
