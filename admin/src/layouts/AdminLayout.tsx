import { Link, NavLink, Outlet } from "react-router-dom";
import { adminLogout, getStoredUser } from "@/lib/adminApi";

const links = [
  { to: "/", label: "Dashboard" },
  { to: "/users", label: "Users" },
  { to: "/videos", label: "Videos" },
  { to: "/channels", label: "Channels" },
];

export default function AdminLayout() {
  const user = getStoredUser();
  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 flex">
      <aside className="w-60 border-r border-slate-700 p-4 flex flex-col gap-2">
        <h2 className="text-lg font-bold mb-4">StreamSphere Admin</h2>
        {links.map((l) => (
          <NavLink
            key={l.to}
            to={l.to}
            end={l.to === "/"}
            className={({ isActive }) =>
              `px-3 py-2 rounded ${isActive ? "bg-indigo-600 text-white" : "text-slate-300 hover:bg-slate-800"}`
            }
          >
            {l.label}
          </NavLink>
        ))}
        <div className="mt-auto pt-4 border-t border-slate-700 text-sm text-slate-400">
          <div className="mb-2">{user?.email}</div>
          <button onClick={adminLogout} className="text-red-400 hover:underline">Logout</button>
        </div>
      </aside>
      <main className="flex-1 p-6 overflow-x-auto">
        <Outlet />
      </main>
    </div>
  );
}
