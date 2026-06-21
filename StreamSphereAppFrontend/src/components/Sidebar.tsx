import { NavLink } from "react-router-dom";
import { Home, Upload } from "lucide-react";
import { cn } from "@/lib/utils";

interface SidebarProps {
  collapsed?: boolean;
}

const mainLinks = [
  { to: "/", icon: Home, label: "Home" },
  { to: "/upload", icon: Upload, label: "Upload" },
];

export function Sidebar({ collapsed = false }: SidebarProps) {
  const linkClass = (isActive: boolean) =>
    cn(
      "flex items-center gap-5 rounded-lg px-3 py-2.5 text-sm transition-colors",
      isActive
        ? "bg-accent font-medium text-foreground"
        : "text-foreground hover:bg-accent"
    );

  if (collapsed) {
    return (
      <aside className="fixed left-0 top-14 z-40 hidden h-[calc(100vh-3.5rem)] w-[72px] flex-col items-center gap-1 overflow-y-auto bg-background py-2 md:flex">
        {mainLinks.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              cn(
                "flex flex-col items-center gap-1 rounded-lg px-1 py-3 text-[10px] transition-colors w-16",
                isActive ? "bg-accent font-medium" : "hover:bg-accent"
              )
            }
          >
            <Icon className="h-5 w-5" />
            {label}
          </NavLink>
        ))}
      </aside>
    );
  }

  return (
    <aside className="fixed left-0 top-14 z-40 hidden h-[calc(100vh-3.5rem)] w-56 flex-col overflow-y-auto bg-background px-2 py-3 md:flex">
      <div className="space-y-1">
        {mainLinks.map(({ to, icon: Icon, label }) => (
          <NavLink key={to} to={to} className={({ isActive }) => linkClass(isActive)}>
            <Icon className="h-5 w-5 shrink-0" />
            {label}
          </NavLink>
        ))}
      </div>
    </aside>
  );
}
