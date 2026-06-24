import type { HTMLAttributes, TdHTMLAttributes, ThHTMLAttributes } from 'react';

export function Table({ className = '', ...props }: HTMLAttributes<HTMLTableElement>) {
  return <table className={`w-full border-separate border-spacing-0 text-left text-sm ${className}`} {...props} />;
}

export function THead({ className = '', ...props }: HTMLAttributes<HTMLTableSectionElement>) {
  return <thead className={`bg-slate-50 text-xs font-semibold uppercase text-slate-500 ${className}`} {...props} />;
}

export function TH({ className = '', ...props }: ThHTMLAttributes<HTMLTableCellElement>) {
  return <th className={`border-b border-slate-200 px-4 py-3 ${className}`} {...props} />;
}

export function TD({ className = '', ...props }: TdHTMLAttributes<HTMLTableCellElement>) {
  return <td className={`border-b border-slate-100 px-4 py-3 ${className}`} {...props} />;
}
