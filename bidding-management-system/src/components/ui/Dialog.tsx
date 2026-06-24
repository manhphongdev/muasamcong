import type { ReactNode } from 'react';
import { X } from 'lucide-react';

interface DialogProps {
  children?: ReactNode;
  open: boolean;
  title: string;
  description?: ReactNode;
  onClose: () => void;
  className?: string;
}

export function Dialog({ children, open, title, description, onClose, className = '' }: DialogProps) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[300] flex items-center justify-center bg-slate-950/40 p-4">
      <div className={`w-full rounded-lg border border-slate-200 bg-white shadow-lg ${className || 'max-w-lg'}`}>
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
          <div>
            <h3 className="text-sm font-semibold text-slate-950">{title}</h3>
            {description ? <div className="mt-0.5 text-xs text-slate-500">{description}</div> : null}
          </div>
          <button type="button" onClick={onClose} className="rounded-md p-1.5 text-slate-500 hover:bg-slate-100" aria-label="Đóng">
            <X className="h-4 w-4" />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}
