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
import axios from "axios";

export const API_GATEWAY_BASE = "https://streamsphere-api-gateway.onrender.com";
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
  let response;
  try {
    response = await axios.request({
      baseURL: API_GATEWAY_BASE,
      url: path,
      method: options.method || "GET",
      headers: headers(options.headers as Record<string, string> | undefined),
      data: options.body,
    });
  } catch (error) {
    if (!axios.isAxiosError(error)) throw error;
    if (!error.response) {
      throw new Error("Backend server is unavailable. Check the API gateway and try again.");
    }
    if (error.response.status === 401 || error.response.status === 403) {
      throw new Error("Unauthorized — admin access required");
    }
    const body = error.response.data;
    throw new Error(body?.error || body?.message || `Request failed: ${error.response.status}`);
  }

  return response.status === 204 ? null : normalizePage(path, response.data);
}

export async function adminLogin(email: string, password: string) {
  let response;
  try {
    response = await axios.post(`${API_GATEWAY_BASE}/api/central/user/login`, {
      email,
      password,
    });
  } catch (error) {
    if (!axios.isAxiosError(error)) throw error;
    if (!error.response) {
      throw new Error("Backend server is unavailable. Check the API gateway and try again.");
    }
    const body = error.response.data;
    throw new Error(body?.error || body?.message || "Login failed");
  }

  const body = response.data;
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
