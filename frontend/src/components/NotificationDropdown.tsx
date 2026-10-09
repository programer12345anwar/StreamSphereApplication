import { useEffect, useRef, useState } from "react";
import { Bell, CheckCheck } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/contexts/AuthContext";
import {
  getNotifications,
  getUnreadNotificationCount,
  markNotificationAsRead,
  markAllNotificationsAsRead,
} from "@/lib/api";

interface NotificationItem {
  id: string | number;
  title?: string;
  message?: string;
  content?: string;
  read?: boolean;
  isRead?: boolean;
  createdAt?: string;
  timestamp?: string;
  link?: string;
  type?: string;
}

function extractItems(response: any): NotificationItem[] {
  if (Array.isArray(response)) return response;
  if (Array.isArray(response?.data)) return response.data;
  if (Array.isArray(response?.content)) return response.content;
  if (Array.isArray(response?.notifications)) return response.notifications;
  if (Array.isArray(response?.data?.content)) return response.data.content;
  return [];
}

export function NotificationDropdown() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [actionLoading, setActionLoading] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const userId = user?.id ? String(user.id) : "";

  async function loadNotifications() {
    if (!userId) return;

    setLoading(true);
    setError("");

    try {
      const [notificationResponse, countResponse] = await Promise.all([
        getNotifications(userId),
        getUnreadNotificationCount(userId),
      ]);

      setItems(extractItems(notificationResponse));

      const rawCount =
        typeof countResponse === "number"
          ? countResponse
          : countResponse?.count ??
            countResponse?.unreadCount ??
            countResponse?.data?.count ??
            countResponse?.data?.unreadCount ??
            0;

      setUnreadCount(Number(rawCount) || 0);
    } catch {
      setError("Unable to load notifications.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (userId) {
      void loadNotifications();
    } else {
      setItems([]);
      setUnreadCount(0);
    }
  }, [userId]);

  useEffect(() => {
    function handleOutsideClick(event: MouseEvent) {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as Node)
      ) {
        setOpen(false);
      }
    }

    document.addEventListener("mousedown", handleOutsideClick);
    return () => document.removeEventListener("mousedown", handleOutsideClick);
  }, []);

  async function handleMarkRead(item: NotificationItem) {
    if (item.read === true || item.isRead === true) return;

    try {
      await markNotificationAsRead(item.id);
      setItems((previous) =>
        previous.map((notification) =>
          notification.id === item.id
            ? { ...notification, read: true, isRead: true }
            : notification
        )
      );
      setUnreadCount((count) => Math.max(0, count - 1));
    } catch {
      setError("Unable to mark notification as read.");
    }
  }

  async function handleMarkAllRead() {
    if (!userId || unreadCount === 0) return;

    setActionLoading(true);
    setError("");

    try {
      await markAllNotificationsAsRead(userId);
      setItems((previous) =>
        previous.map((item) => ({ ...item, read: true, isRead: true }))
      );
      setUnreadCount(0);
    } catch {
      setError("Unable to mark all notifications as read.");
    } finally {
      setActionLoading(false);
    }
  }

  async function handleNotificationClick(item: NotificationItem) {
    await handleMarkRead(item);
    setOpen(false);

    if (item.link && item.link.startsWith("/")) {
      navigate(item.link);
    }
  }

  if (!user) return null;

  return (
    <div className="relative" ref={containerRef}>
      <Button
        type="button"
        variant="ghost"
        size="icon"
        className="relative"
        aria-label={`Notifications${unreadCount ? `, ${unreadCount} unread` : ""}`}
        aria-expanded={open}
        onClick={() => {
          const nextOpen = !open;
          setOpen(nextOpen);
          if (nextOpen) void loadNotifications();
        }}
      >
        <Bell className="h-5 w-5" />
        {unreadCount > 0 && (
          <span className="absolute -right-1 -top-1 flex h-5 min-w-5 items-center justify-center rounded-full bg-destructive px-1 text-xs text-destructive-foreground">
            {unreadCount > 99 ? "99+" : unreadCount}
          </span>
        )}
      </Button>

      {open && (
        <div
          role="dialog"
          aria-label="Notifications"
          className="absolute right-0 top-full z-50 mt-2 w-[min(22rem,calc(100vw-2rem))] overflow-hidden rounded-xl border bg-background text-foreground shadow-lg"
        >
          <div className="flex items-center justify-between gap-2 border-b p-4">
            <h2 className="font-semibold">Notifications</h2>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              disabled={actionLoading || unreadCount === 0}
              onClick={() => void handleMarkAllRead()}
              className="gap-1"
            >
              <CheckCheck className="h-4 w-4" />
              Mark all read
            </Button>
          </div>

          {error && (
            <div className="p-3 text-sm text-destructive" role="alert">
              {error}
              <button
                type="button"
                className="ml-2 underline"
                onClick={() => void loadNotifications()}
              >
                Retry
              </button>
            </div>
          )}

          <div className="max-h-96 overflow-y-auto">
            {loading && items.length === 0 ? (
              <p className="p-6 text-center text-sm text-muted-foreground">
                Loading notifications...
              </p>
            ) : items.length === 0 ? (
              <p className="p-6 text-center text-sm text-muted-foreground">
                No notifications yet.
              </p>
            ) : (
              items.map((item) => {
                const isRead = item.read === true || item.isRead === true;

                return (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => void handleNotificationClick(item)}
                    className={`block w-full border-b p-4 text-left transition-colors hover:bg-muted ${
                      isRead ? "" : "bg-muted/40"
                    }`}
                  >
                    <div className="flex items-start gap-3">
                      {!isRead && (
                        <span className="mt-2 h-2 w-2 shrink-0 rounded-full bg-primary" />
                      )}
                      <div className="min-w-0 flex-1">
                        <p className="font-medium">
                          {item.title || item.type || "Notification"}
                        </p>
                        <p className="mt-1 break-words text-sm text-muted-foreground">
                          {item.message || item.content || "You have a new notification."}
                        </p>
                        {(item.createdAt || item.timestamp) && (
                          <p className="mt-1 text-xs text-muted-foreground">
                            {new Date(
                              item.createdAt || item.timestamp!
                            ).toLocaleString()}
                          </p>
                        )}
                      </div>
                    </div>
                  </button>
                );
              })
            )}
          </div>
        </div>
      )}
    </div>
  );
}
