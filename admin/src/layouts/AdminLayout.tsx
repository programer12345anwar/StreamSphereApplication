import { NavLink, Outlet } from "react-router-dom";
import { adminLogout, getStoredUser } from "@/lib/adminApi";
import { LayoutDashboard, Users, TvMinimalPlay, Clapperboard, LogOut, Flag, Menu, X } from "lucide-react";
import { useState } from "react";

const links = [
  { to: "/", label: "Dashboard", icon: LayoutDashboard },
  { to: "/users", label: "Users", icon: Users },
  { to: "/videos", label: "Videos", icon: Clapperboard },
  { to: "/channels", label: "Channels", icon: TvMinimalPlay },
  { to: "/reports", label: "Reports", icon: Flag },
];

export default function AdminLayout() {
  const user = getStoredUser();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const navigation = (
    <nav aria-label="Admin navigation" className="flex flex-col gap-1.5">
      {links.map((link) => (
        <NavLink
          key={link.to}
          to={link.to}
          end={link.to === "/"}
          onClick={() => setMobileMenuOpen(false)}
          className={({ isActive }) =>
            `flex min-h-11 items-center gap-3 rounded-lg px-3 py-2.5 font-medium transition-all duration-200 ${
              isActive
                ? "bg-indigo-500/10 text-indigo-400 shadow-[inset_0_0_0_1px_rgba(99,102,241,0.2)]"
                : "text-slate-400 hover:bg-slate-800/50 hover:text-slate-200"
            }`
          }
        >
          <link.icon className="h-5 w-5 shrink-0" />
          {link.label}
        </NavLink>
      ))}
    </nav>
  );

  const accountFooter = (
    <div className="mt-auto border-t border-slate-800/60 pt-5">
      <div className="mb-4 px-3">
        <div className="mb-1 text-xs font-semibold uppercase tracking-wider text-slate-500">Logged in as</div>
        <div className="truncate text-sm font-medium text-slate-300">{user?.email}</div>
      </div>
      <button
        onClick={adminLogout}
        className="flex min-h-11 w-full items-center gap-3 rounded-lg px-3 py-2.5 text-slate-400 transition-colors hover:bg-red-500/10 hover:text-red-400"
      >
        <LogOut className="h-5 w-5" />
        Sign Out
      </button>
    </div>
  );

  return (
    <div className="min-h-screen overflow-x-hidden bg-slate-950 text-slate-100 selection:bg-indigo-500/30">
      <header className="fixed inset-x-0 top-0 z-40 flex h-14 items-center gap-3 border-b border-slate-800/60 bg-slate-950/95 px-3 backdrop-blur-xl md:hidden">
        <button
          type="button"
          aria-label={mobileMenuOpen ? "Close admin navigation" : "Open admin navigation"}
          aria-expanded={mobileMenuOpen}
          onClick={() => setMobileMenuOpen((open) => !open)}
          className="flex h-10 w-10 items-center justify-center rounded-lg text-slate-200 hover:bg-slate-800"
        >
          {mobileMenuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
        </button>
        <div className="flex min-w-0 items-center gap-2">
          <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-gradient-to-br from-indigo-500 to-purple-600">
            <TvMinimalPlay className="h-5 w-5 text-white" />
          </span>
          <span className="truncate font-bold text-slate-100">StreamSphere Admin</span>
        </div>
      </header>

      {mobileMenuOpen && (
        <button
          type="button"
          aria-label="Close admin navigation"
          onClick={() => setMobileMenuOpen(false)}
          className="fixed inset-0 z-40 bg-black/60 md:hidden"
        />
      )}
      <aside
        className={`fixed inset-y-0 left-0 z-50 flex w-[min(18rem,85vw)] flex-col gap-2 border-r border-slate-800/60 bg-slate-900 p-4 shadow-2xl transition-transform duration-200 md:hidden ${
          mobileMenuOpen ? "visible translate-x-0" : "invisible -translate-x-full"
        }`}
        aria-label="Mobile admin navigation"
        aria-hidden={!mobileMenuOpen}
      >
        <div className="mb-5 flex items-center gap-3 px-2 py-4">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-gradient-to-br from-indigo-500 to-purple-600">
            <TvMinimalPlay className="h-5 w-5 text-white" />
          </div>
          <h2 className="bg-gradient-to-r from-slate-100 to-slate-400 bg-clip-text text-xl font-bold text-transparent">StreamSphere</h2>
        </div>
        {navigation}
        {accountFooter}
      </aside>

      <aside className="fixed inset-y-0 left-0 z-20 hidden w-64 flex-col gap-2 border-r border-slate-800/60 bg-slate-900/50 p-4 shadow-2xl backdrop-blur-xl md:flex">
        <div className="mb-6 flex items-center gap-3 px-2 py-4">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-gradient-to-br from-indigo-500 to-purple-600 shadow-lg shadow-indigo-500/20">
            <TvMinimalPlay className="h-5 w-5 text-white" />
          </div>
          <h2 className="bg-gradient-to-r from-slate-100 to-slate-400 bg-clip-text text-xl font-bold text-transparent">StreamSphere</h2>
        </div>
        {navigation}
        {accountFooter}
      </aside>

      <main className="min-h-screen min-w-0 overflow-x-hidden bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-slate-900 via-slate-950 to-slate-950 pt-14 md:ml-64 md:pt-0">
        <div className="mx-auto min-h-screen w-full min-w-0 max-w-7xl px-4 py-5 sm:px-6 sm:py-7 lg:p-8">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
