import React from 'react';

interface MetricCardProps {
  label: string;
  value: React.ReactNode;
  icon?: React.ReactNode;
  tone?: 'neutral' | 'info' | 'success' | 'warning' | 'danger';
  active?: boolean;
  onClick?: () => void;
}

const tones = {
  neutral: 'border-slate-200 bg-white text-slate-600',
  info: 'border-sky-200 bg-sky-50 text-sky-700',
  success: 'border-emerald-200 bg-emerald-50 text-emerald-700',
  warning: 'border-amber-200 bg-amber-50 text-amber-700',
  danger: 'border-rose-200 bg-rose-50 text-rose-700'
};

export function MetricCard({ label, value, icon, tone = 'neutral', active = false, onClick }: MetricCardProps) {
  const Component = onClick ? 'button' : 'div';
  return (
    <Component
      onClick={onClick}
      className={`rounded-lg border p-4 text-left shadow-sm transition-colors ${tones[tone]} ${
        active ? 'border-slate-900 ring-2 ring-slate-900/5' : ''
      } ${onClick ? 'cursor-pointer hover:border-slate-300 hover:bg-white' : ''}`}
    >
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-medium text-slate-500">{label}</p>
          <div className="mt-2 text-2xl font-semibold tracking-normal text-slate-950">{value}</div>
        </div>
        {icon && <div className="rounded-md border border-current/15 bg-white/70 p-2 opacity-90">{icon}</div>}
      </div>
    </Component>
  );
}
