const API_GATEWAY_BASE = (import.meta.env.VITE_API_GATEWAY_URL || "http://localhost:8080").replace(/\/+$/, "");
const TOKEN_KEY = "yt_token";

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function authHeaders(extraHeaders = {}) {
  const token = getToken();
  if (!token) {
    return { ...extraHeaders };
  }
  return {
    ...extraHeaders,
    Authorization: `Bearer ${token}`,
  };
}

async function parseResponse(response) {
  const contentType = response.headers.get("content-type") || "";
  const isJson = contentType.includes("application/json");
  const rawBody = await response.text();
  let payload = rawBody;

  if (isJson && rawBody) {
    try {
      payload = JSON.parse(rawBody);
    } catch {
      payload = rawBody;
    }
  }

  if (!response.ok) {
    const message =
      (payload && typeof payload === "object" && (payload.message || payload.error)) ||
      (typeof payload === "string" && payload) ||
      `Request failed with status ${response.status}`;
    throw new Error(message);
  }

  return payload;
}

async function apiRequest(path, options = {}) {
  const response = await fetch(`${API_GATEWAY_BASE}${path}`, options);
  return parseResponse(response);
}

export function getAuthToken() {
  return getToken();
}

export { TOKEN_KEY, API_GATEWAY_BASE };

export async function registerUser(data) {
  return apiRequest("/api/central/user/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
}

export async function loginUser(data) {
  return apiRequest("/api/central/user/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
}

export async function validateToken(token) {
  return apiRequest(`/api/v1/central/security/validate-token/${encodeURIComponent(token)}`, {
    headers: authHeaders(),
  });
}

export async function createChannel(data) {
  return apiRequest("/api/v1/central/channel/create", {
    method: "POST",
    headers: authHeaders({ "Content-Type": "application/json" }),
    body: JSON.stringify(data),
  });
}

export async function getMyChannel(email) {
  return apiRequest(`/api/v1/central/channel/my-channel?email=${encodeURIComponent(email)}`, {
    headers: authHeaders(),
  });
}

export async function uploadVideo(channelId, videoFile, videoDetails) {
  const formData = new FormData();
  formData.append("videoFile", videoFile);
  formData.append(
    "videodetails",
    new Blob([JSON.stringify(videoDetails)], { type: "application/json" }),
  );

  return apiRequest(`/api/v1/video/upload?channelId=${encodeURIComponent(channelId)}`, {
    method: "POST",
    headers: authHeaders(),
    body: formData,
  });
}

export async function getVideoFeed(limit = 30) {
  return apiRequest(`/api/v1/central/videos?limit=${limit}`);
}

export async function getVideoById(videoId) {
  return apiRequest(`/api/v1/central/videos/${encodeURIComponent(videoId)}`);
}

export async function subscribeToChannel(channelId, userId) {
  return apiRequest(
    `/api/v1/central/channel/${channelId}/subscribe?userId=${encodeURIComponent(userId)}`,
    {
      method: "PUT",
      headers: authHeaders(),
    },
  );
}
