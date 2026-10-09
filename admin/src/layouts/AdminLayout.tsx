import { NavLink, Outlet } from "react-router-dom";
import { adminLogout, getStoredUser } from "@/lib/adminApi";
import { LayoutDashboard, Users, TvMinimalPlay, Clapperboard, LogOut, Flag } from "lucide-react";

const links = [
  { to: "/", label: "Dashboard", icon: LayoutDashboard },
  { to: "/users", label: "Users", icon: Users },
  { to: "/videos", label: "Videos", icon: Clapperboard },
  { to: "/channels", label: "Channels", icon: TvMinimalPlay },
  { to: "/reports", label: "Reports", icon: Flag },
];

export default function AdminLayout() {
  const user = getStoredUser();
  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex selection:bg-indigo-500/30">
      <aside className="w-64 border-r border-slate-800/60 bg-slate-900/50 backdrop-blur-xl p-4 flex flex-col gap-2 shadow-2xl z-10">
        <div className="flex items-center gap-3 px-2 py-4 mb-6">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center shadow-lg shadow-indigo-500/20">
            <TvMinimalPlay className="w-5 h-5 text-white" />
          </div>
          <h2 className="text-xl font-bold bg-gradient-to-r from-slate-100 to-slate-400 bg-clip-text text-transparent">StreamSphere</h2>
        </div>
        
        <div className="flex flex-col gap-1.5 flex-1">
          {links.map((l) => (
            <NavLink
              key={l.to}
              to={l.to}
              end={l.to === "/"}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-lg transition-all duration-200 font-medium ${
                  isActive 
                    ? "bg-indigo-500/10 text-indigo-400 shadow-[inset_0_0_0_1px_rgba(99,102,241,0.2)]" 
                    : "text-slate-400 hover:bg-slate-800/50 hover:text-slate-200"
                }`
              }
            >
              <l.icon className="w-5 h-5" />
              {l.label}
            </NavLink>
          ))}
        </div>

        <div className="mt-auto pt-6 border-t border-slate-800/60">
          <div className="px-3 mb-4">
            <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">Logged in as</div>
            <div className="text-sm text-slate-300 truncate font-medium">{user?.email}</div>
          </div>
          <button 
            onClick={adminLogout} 
            className="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-slate-400 hover:bg-red-500/10 hover:text-red-400 transition-colors"
          >
            <LogOut className="w-5 h-5" />
            Sign Out
          </button>
        </div>
      </aside>
      
      <main className="flex-1 overflow-x-hidden overflow-y-auto bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-slate-900 via-slate-950 to-slate-950">
        <div className="p-8 max-w-7xl mx-auto">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
