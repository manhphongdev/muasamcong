interface ProgressBarProps {
  value: number;
  tone?: 'success' | 'warning' | 'danger' | 'info' | 'neutral';
  className?: string;
}

const tones = {
  success: 'bg-emerald-500',
  warning: 'bg-amber-500',
  danger: 'bg-rose-500',
  info: 'bg-indigo-500',
  neutral: 'bg-slate-500'
};

export function ProgressBar({ value, tone = 'info', className = '' }: ProgressBarProps) {
  const safeValue = Math.min(Math.max(value || 0, 0), 100);
  return (
    <div className={`h-2 w-full rounded-full bg-slate-100 overflow-hidden ${className}`}>
      <div className={`h-full rounded-full ${tones[tone]}`} style={{ width: `${safeValue}%` }} />
    </div>
  );
}
