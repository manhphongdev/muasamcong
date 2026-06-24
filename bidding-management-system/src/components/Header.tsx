/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
import { FileSpreadsheet, RefreshCw, FolderSearch, UserCheck } from 'lucide-react';

interface HeaderProps {
  onImportClick: () => void;
  onRefreshClick: () => void;
  isRefreshing: boolean;
}

export default function Header({ onImportClick, onRefreshClick, isRefreshing }: HeaderProps) {
  return (
    <header 
      className="bg-white border-b border-slate-205/60 py-5 px-6 sm:px-8 flex flex-col md:flex-row md:items-center justify-between gap-4 shadow-3xs"
      id="app-header"
    >
      {/* Title & Short Description */}
      <div className="space-y-1.5">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-800 flex items-center justify-center shadow-3xs">
            <FolderSearch className="w-5.5 h-5.5" />
          </div>
          <div>
            <h1 className="font-sans font-bold text-xl sm:text-2xl text-slate-900 tracking-tight">
              Theo dõi gói thầu
            </h1>
            <p className="text-xs sm:text-sm text-slate-500 font-medium leading-relaxed">
              Theo dõi TBMT, thư mục THHD, thời hạn đóng thầu và kết quả chọn nhà thầu
            </p>
          </div>
        </div>
      </div>

      {/* Main Business Action Triggers */}
      <div className="flex items-center flex-wrap gap-2.5">
        <button
          id="btn-import-thhd-header"
          onClick={onImportClick}
          className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2.5 rounded-lg text-sm font-semibold shadow-xs shadow-indigo-500/20 hover:shadow-sm active:scale-98 transition-all"
        >
          <FileSpreadsheet className="w-4 h-4 text-emerald-400" />
          <span>Import thư mục THHD</span>
        </button>

        <button
          id="btn-refresh-data-header"
          onClick={onRefreshClick}
          disabled={isRefreshing}
          className="flex items-center gap-2 bg-white border border-slate-250 hover:bg-slate-50 text-slate-700 px-4 py-2.5 rounded-lg text-sm font-semibold shadow-3xs hover:shadow-2xs active:scale-98 transition-all disabled:opacity-60"
        >
          <RefreshCw className={`w-4 h-4 text-slate-500 ${isRefreshing ? 'animate-spin' : ''}`} />
          <span>Cập nhật dữ liệu</span>
        </button>

        <div className="hidden sm:flex items-center gap-2 bg-slate-50 border border-slate-200/60 py-1 px-3 rounded-lg ml-1">
          <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></div>
          <span className="text-[11px] text-slate-600 font-bold font-mono uppercase">MSC GATEWAY LIVE</span>
        </div>
      </div>
    </header>
  );
}
