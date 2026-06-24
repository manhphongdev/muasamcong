import {
  ClipboardList,
  PanelLeftClose,
  PanelLeftOpen,
  UploadCloud
} from 'lucide-react';
import logoSrc from '@/assets/image/logo.png';

interface SidebarProps {
  activeSection: string;
  setActiveSection: (sec: string) => void;
  packagesCount: number;
  downloadCount: number;
  collapsed: boolean;
  onToggleCollapsed: () => void;
  disabled?: boolean;
}

export default function Sidebar({
  activeSection,
  setActiveSection,
  packagesCount,
  downloadCount,
  collapsed,
  onToggleCollapsed,
  disabled = false
}: SidebarProps) {
  const navItems = [
    { id: 'import_center', label: 'Thêm gói thầu', icon: UploadCloud, badge: null },
    { id: 'thhd_tracking', label: 'Theo dõi gói thầu', icon: ClipboardList, badge: packagesCount || null },
  ];

  return (
    <aside
      className={`${collapsed ? 'w-[76px] px-3' : 'w-[248px] px-3'} fixed left-0 top-0 z-50 flex h-screen flex-col border-r border-slate-200 bg-white py-4 text-slate-700 transition-all duration-200 ${
        disabled ? 'pointer-events-none select-none opacity-40' : ''
      }`}
      id="admin-sidebar"
    >
      <div className={`relative mb-5 flex items-center px-2 ${collapsed ? 'justify-center' : 'gap-3'} select-none`}>
        <div className="relative flex h-10 w-10 items-center justify-center overflow-hidden rounded-lg border border-slate-200 bg-white p-1.5">
          <img src={logoSrc} alt="MSC Monitor logo" className="h-full w-full object-contain" />
        </div>
        {!collapsed && (
          <div>
            <h1 className="font-sans text-sm font-semibold leading-none text-slate-950">MSC Sync</h1>
            <p className="mt-1 text-xs text-slate-500">Quản lý gói thầu</p>
          </div>
        )}
        <button
          type="button"
          onClick={onToggleCollapsed}
          disabled={disabled}
          title={collapsed ? 'Mở rộng sidebar' : 'Thu gọn sidebar'}
          aria-label={collapsed ? 'Mở rộng sidebar' : 'Thu gọn sidebar'}
          className={`${collapsed ? 'absolute -right-5 top-1' : 'ml-auto'} flex h-8 w-8 items-center justify-center rounded-md border border-slate-200 bg-white text-slate-500 transition-colors hover:bg-slate-50 hover:text-slate-900 disabled:pointer-events-none disabled:opacity-50`}
        >
          {collapsed ? <PanelLeftOpen className="h-4 w-4" /> : <PanelLeftClose className="h-4 w-4" />}
        </button>
      </div>

      <div className="flex-grow overflow-y-auto">
        {!collapsed && <p className="mb-2 px-2 text-xs font-medium text-slate-500">Điều hướng</p>}
        <nav className="space-y-1">
          {navItems.map((item) => {
            const IconComp = item.icon;
            const isActive = activeSection === item.id;
            return (
              <button
                key={item.id}
                id={`sidebar-tab-${item.id}`}
                onClick={() => setActiveSection(item.id)}
                className={`group flex w-full items-center justify-between rounded-md px-2.5 py-2 text-left outline-none transition-colors ${
                  isActive
                    ? 'bg-slate-900 font-semibold text-white'
                    : 'text-slate-600 hover:bg-slate-100 hover:text-slate-950'
                }`}
                title={collapsed ? item.label : undefined}
              >
                <div className={`flex items-center ${collapsed ? 'w-full justify-center' : 'gap-2.5'}`}>
                  <span className={`${collapsed ? 'h-9 w-9' : 'h-8 w-8'} flex items-center justify-center rounded-md transition-colors ${
                    isActive ? 'bg-white/10 text-white' : 'bg-white text-slate-500 ring-1 ring-slate-200 group-hover:text-slate-900'
                  }`}>
                    <IconComp className="h-[17px] w-[17px]" />
                  </span>
                  {!collapsed && <span className="text-sm font-medium">{item.label}</span>}
                </div>
                {!collapsed && item.badge !== null && (
                  <span className={`rounded-md px-2 py-0.5 font-mono text-[11px] font-semibold ${
                    isActive ? 'bg-white/10 text-white' : 'bg-slate-100 text-slate-500'
                  }`}>
                    {item.badge}
                  </span>
                )}
              </button>
            );
          })}
        </nav>
      </div>
    </aside>
  );
}
