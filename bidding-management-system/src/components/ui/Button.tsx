import type { MouseEventHandler, ReactNode } from 'react';

type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'ghost';
type ButtonSize = 'sm' | 'md';

interface ButtonProps {
  variant?: ButtonVariant;
  size?: ButtonSize;
  children?: ReactNode;
  className?: string;
  disabled?: boolean;
  title?: string;
  type?: 'button' | 'submit' | 'reset';
  onClick?: MouseEventHandler<HTMLButtonElement>;
}

const variants: Record<ButtonVariant, string> = {
  primary: 'bg-indigo-600 text-white border-indigo-600 hover:bg-indigo-700 shadow-indigo-500/20',
  secondary: 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50',
  danger: 'bg-rose-600 text-white border-rose-600 hover:bg-rose-700',
  ghost: 'bg-transparent text-slate-600 border-transparent hover:bg-slate-100'
};

const sizes: Record<ButtonSize, string> = {
  sm: 'px-3 py-1.5 text-[11px]',
  md: 'px-4 py-2 text-xs'
};

export function Button({ variant = 'secondary', size = 'md', className = '', type = 'button', ...props }: ButtonProps) {
  return (
    <button
      type={type}
      className={`inline-flex items-center justify-center gap-1.5 rounded-xl border font-bold transition-all cursor-pointer disabled:cursor-not-allowed disabled:opacity-50 shadow-3xs ${variants[variant]} ${sizes[size]} ${className}`}
      {...props}
    />
  );
}
