import { Link, useNavigate } from "react-router-dom";
import { Search, Upload, Bell, Menu, User, LogOut, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/contexts/AuthContext";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useState } from "react";

interface HeaderProps {
  onToggleSidebar?: () => void;
}

export function Header({ onToggleSidebar }: HeaderProps) {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState("");

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/search?q=${encodeURIComponent(searchQuery.trim())}`);
    }
  };

  return (
    <header className="fixed top-0 left-0 right-0 z-50 flex h-14 items-center justify-between gap-4 bg-background px-4">
      {/* Left */}
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={onToggleSidebar} className="text-foreground">
          <Menu className="h-5 w-5" />
        </Button>
        <Link to="/" className="flex items-center gap-1">
          <div className="flex items-center">
            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-primary">
              <svg viewBox="0 0 24 24" className="h-4 w-4 fill-primary-foreground">
                <path d="M10 8l6 4-6 4V8z" />
              </svg>
            </div>
            <span className="ml-1 text-lg font-semibold text-foreground tracking-tight">YouTube</span>
          </div>
        </Link>
      </div>

      {/* Center - Search */}
      <form onSubmit={handleSearch} className="flex max-w-2xl flex-1 items-center">
        <div className="flex flex-1">
          <Input
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search"
            className="rounded-r-none border-r-0 bg-background focus-visible:ring-1 focus-visible:ring-primary/50"
          />
          <Button
            type="submit"
            variant="secondary"
            size="icon"
            className="rounded-l-none border border-input px-6"
          >
            <Search className="h-4 w-4" />
          </Button>
        </div>
      </form>

      {/* Right */}
      <div className="flex items-center gap-1">
        {isAuthenticated ? (
          <>
            <Button variant="ghost" size="icon" onClick={() => navigate("/upload")} className="text-foreground">
              <Upload className="h-5 w-5" />
            </Button>
            <Button variant="ghost" size="icon" className="text-foreground">
              <Bell className="h-5 w-5" />
            </Button>
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" className="rounded-full">
                  <Avatar className="h-8 w-8">
                    <AvatarFallback className="bg-primary text-primary-foreground text-sm">
                      {user?.name?.charAt(0)?.toUpperCase() || "U"}
                    </AvatarFallback>
                  </Avatar>
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56">
                <div className="px-3 py-2">
                  <p className="text-sm font-medium">{user?.name}</p>
                  <p className="text-xs text-muted-foreground">{user?.email}</p>
                </div>
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={() => navigate("/channel/create")}>
                  <Plus className="mr-2 h-4 w-4" /> Create Channel
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={logout}>
                  <LogOut className="mr-2 h-4 w-4" /> Sign out
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </>
        ) : (
          <Button
            variant="outline"
            onClick={() => navigate("/login")}
            className="gap-2 rounded-full border-primary/50 text-primary hover:bg-primary/10"
          >
            <User className="h-4 w-4" />
            Sign in
          </Button>
        )}
      </div>
    </header>
  );
}
