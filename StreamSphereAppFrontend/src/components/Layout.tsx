import { Header } from "@/components/Header";
import { Sidebar } from "@/components/Sidebar";
import { ReactNode, useState } from "react";

export function Layout({ children }: { children: ReactNode }) {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  return (
    <div className="min-h-screen bg-background">
      <Header onToggleSidebar={() => setSidebarCollapsed(!sidebarCollapsed)} />
      <Sidebar collapsed={sidebarCollapsed} />
      <main
        className={`pt-14 transition-all duration-200 ${
          sidebarCollapsed ? "ml-[72px]" : "ml-56"
        }`}
      >
        {children}
      </main>
    </div>
  );
}
