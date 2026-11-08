import { Link, NavLink, useNavigate } from "react-router-dom";
import { Home, ListVideo, Menu, Moon, Plus, Search, Smartphone, Sun, Upload, User, LogOut, Monitor } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/contexts/AuthContext";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { BrandLogo } from "@/components/BrandLogo";
import { NotificationDropdown } from "@/components/NotificationDropdown";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { FormEvent, useState } from "react";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { useTheme } from "next-themes";

interface HeaderProps {
  onToggleSidebar?: () => void;
}

export function Header({ onToggleSidebar }: HeaderProps) {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState("");
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const { theme, setTheme } = useTheme();

  const handleSearch = (e: FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/search?q=${encodeURIComponent(searchQuery.trim())}`);
    }
  };

  return (
    <header className="fixed left-0 right-0 top-0 z-50 flex h-14 min-w-0 items-center justify-between gap-2 bg-background px-2 sm:gap-4 sm:px-4">
      <div className="flex shrink-0 items-center gap-1 sm:gap-4">
        <Sheet open={mobileNavOpen} onOpenChange={setMobileNavOpen}>
          <SheetTrigger asChild>
            <Button
              variant="ghost"
              size="icon"
              className="text-foreground md:hidden"
              aria-label="Open navigation menu"
            >
              <Menu className="h-5 w-5" />
            </Button>
          </SheetTrigger>
          <SheetContent side="left" className="w-[min(18rem,85vw)] p-4">
            <SheetTitle className="mb-5 text-left">Navigation</SheetTitle>
            <SheetDescription className="sr-only">
              Navigate to StreamSphere pages.
            </SheetDescription>
            <nav aria-label="Mobile navigation" className="flex flex-col gap-1">
              {[
                { to: "/", label: "Home", Icon: Home },
                { to: "/shorts", label: "Shorts", Icon: Smartphone },
                { to: "/playlists", label: "Playlists", Icon: ListVideo },
                { to: "/upload", label: "Upload", Icon: Upload },
              ].map(({ to, label, Icon }) => (
                <NavLink
                  key={to}
                  to={to}
                  onClick={() => setMobileNavOpen(false)}
                  className={({ isActive }) =>
                    `flex min-h-11 items-center gap-4 rounded-lg px-3 text-sm transition-colors ${
                      isActive
                        ? "bg-accent font-medium text-foreground"
                        : "text-foreground hover:bg-accent"
                    }`
                  }
                >
                  <Icon className="h-5 w-5" />
                  {label}
                </NavLink>
              ))}
            </nav>
            <div className="my-4 border-t" />
            <p className="mb-2 px-3 text-xs font-medium text-muted-foreground">Appearance</p>
            <div className="grid grid-cols-3 gap-2">
              {[
                { value: "light", label: "Light", Icon: Sun },
                { value: "dark", label: "Dark", Icon: Moon },
                { value: "system", label: "System", Icon: Monitor },
              ].map(({ value, label, Icon }) => (
                <button
                  key={value}
                  type="button"
                  aria-pressed={theme === value}
                  onClick={() => setTheme(value)}
                  className={`flex min-h-16 flex-col items-center justify-center gap-1 rounded-lg border text-xs transition-colors ${
                    theme === value ? "border-primary bg-accent text-foreground" : "border-border text-muted-foreground"
                  }`}
                >
                  <Icon className="h-4 w-4" />
                  {label}
                </button>
              ))}
            </div>
          </SheetContent>
        </Sheet>
        <Button
          variant="ghost"
          size="icon"
          onClick={onToggleSidebar}
          className="hidden text-foreground md:inline-flex"
          aria-label="Toggle navigation"
        >
          <Menu className="h-5 w-5" />
        </Button>
        <Link to="/" aria-label="StreamSphere home">
          <BrandLogo labelClassName="hidden sm:inline" />
        </Link>
      </div>

      <form onSubmit={handleSearch} className="flex min-w-0 max-w-2xl flex-1 items-center">
        <div className="flex min-w-0 flex-1">
          <Input
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search"
            className="min-w-0 rounded-r-none border-r-0 bg-background focus-visible:ring-1 focus-visible:ring-primary/50"
          />
          <Button
            type="submit"
            variant="secondary"
            size="icon"
            className="rounded-l-none border border-input px-6"
            aria-label="Search videos"
          >
            <Search className="h-4 w-4" />
          </Button>
        </div>
      </form>

      <div className="flex shrink-0 items-center gap-0.5 sm:gap-1">
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              variant="ghost"
              size="icon"
              className="hidden text-foreground md:inline-flex"
              aria-label="Choose appearance"
            >
              {theme === "dark" ? <Moon className="h-5 w-5" /> : <Sun className="h-5 w-5" />}
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-40">
            <DropdownMenuLabel>Appearance</DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuRadioGroup value={theme} onValueChange={setTheme}>
              <DropdownMenuRadioItem value="light"><Sun className="mr-2 h-4 w-4" />Light</DropdownMenuRadioItem>
              <DropdownMenuRadioItem value="dark"><Moon className="mr-2 h-4 w-4" />Dark</DropdownMenuRadioItem>
              <DropdownMenuRadioItem value="system"><Monitor className="mr-2 h-4 w-4" />System</DropdownMenuRadioItem>
            </DropdownMenuRadioGroup>
          </DropdownMenuContent>
        </DropdownMenu>
        {isAuthenticated ? (
          <>
            <Button
              variant="ghost"
              size="icon"
              onClick={() => navigate("/upload")}
              className="text-foreground"
              aria-label="Upload video"
            >
              <Upload className="h-5 w-5" />
            </Button>
            <NotificationDropdown />
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" className="rounded-full" aria-label="Open account menu">
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
