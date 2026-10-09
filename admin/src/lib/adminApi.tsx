import {
  createElement,
  type ReactNode,
} from "react";
import {
  useQuery,
  useMutation,
  useQueryClient,
  QueryClient,
  QueryClientProvider,
} from "@tanstack/react-query";

const API_BASE = (import.meta.env.VITE_API_GATEWAY_URL ?? (typeof window !== "undefined" ? `${window.location.protocol}//${window.location.hostname}:8080` : "http://localhost:8080")).replace(/\/+$/, "");
export const TOKEN_KEY = "streamsphere_admin_token";
export const USER_KEY = "streamsphere_admin_user";

export const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, staleTime: 30_000 } },
});

export function getToken() {
  try { return localStorage.getItem(TOKEN_KEY); } catch { return null; }
}

export function getStoredUser() {
  try { return JSON.parse(localStorage.getItem(USER_KEY) || "null"); } catch { return null; }
}

function headers(extra = {}) {
  const token = getToken();
  return token ? { ...extra, Authorization: `Bearer ${token}` } : { ...extra };
}

async function parseBody(res: Response) {
  const raw = await res.text();
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return raw;
  }
}

function normalizeVideo(video: any) {
  if (!video || typeof video !== "object") return video;
  const title = video.title ?? video.name ?? "Untitled video";
  return {
    ...video,
    title,
    name: video.name ?? title,
    uploadedAt: video.uploadedAt ?? video.uploadDateTime ?? null,
    channelName: video.channelName ?? video.channel?.name ?? "Unknown channel",
  };
}

function normalizePage(path: string, body: any) {
  if (!body || !Array.isArray(body.content)) return body;
  if (path.startsWith("/api/v1/admin/videos")) {
    return { ...body, content: body.content.map(normalizeVideo) };
  }
  return body;
}

export async function request(path: string, options: RequestInit = {}) {
  let res: Response;
  try {
    res = await fetch(`${API_BASE}${path}`, { ...options, headers: headers(options.headers as Record<string, string> | undefined) });
  } catch {
    throw new Error("Backend server is unavailable. Start the API gateway and confirm the backend is running on port 8080.");
  }

  const body = await parseBody(res);
  if (res.status === 401 || res.status === 403) {
    throw new Error("Unauthorized — admin access required");
  }
  if (!res.ok) {
    throw new Error(body?.error || body?.message || `Request failed: ${res.status}`);
  }
  return res.status === 204 ? null : normalizePage(path, body);
}

export async function adminLogin(email: string, password: string) {
  let res: Response;
  try {
    res = await fetch(`${API_BASE}/api/central/user/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
    });
  } catch {
    throw new Error("Backend server is unavailable. Start the API gateway and confirm the backend is running on port 8080.");
  }

  const body = await parseBody(res);
  if (!res.ok) throw new Error(body?.error || "Login failed");
  if (body.role !== "ADMIN") throw new Error("This account is not an admin");
  localStorage.setItem(TOKEN_KEY, body.token);
  localStorage.setItem(USER_KEY, JSON.stringify({ email, role: body.role }));
  return body;
}

export function adminLogout() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  window.location.href = "/login";
}

export function AdminQueryProvider({ children }: { children: ReactNode }) {
  return createElement(QueryClientProvider, { client: queryClient }, children);
}

export { useQuery, useMutation, useQueryClient };
