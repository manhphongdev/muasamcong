import type { Key } from 'react';

interface SkeletonProps {
  key?: Key;
  className?: string;
}

export function Skeleton({ className = '' }: SkeletonProps) {
  return <div className={`animate-pulse rounded-xl bg-slate-100 ${className}`} />;
}

export function TableSkeleton({ rows = 8, columns = 7 }: { rows?: number; columns?: number }) {
  return (
    <div className="space-y-3 p-6">
      {Array.from({ length: rows }, (_, row) => (
        <div key={row} className="grid gap-3" style={{ gridTemplateColumns: `repeat(${columns}, minmax(0, 1fr))` }}>
          {Array.from({ length: columns }, (_, col) => <Skeleton key={col} className="h-9" />)}
        </div>
      ))}
    </div>
  );
}
