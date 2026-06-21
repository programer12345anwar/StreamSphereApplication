const DEFAULT_DEV_API_GATEWAY_URL = "http://localhost:8080";
const configuredGatewayUrl = import.meta.env.VITE_API_GATEWAY_URL?.trim();
const API_GATEWAY_BASE = (
  configuredGatewayUrl || (import.meta.env.DEV ? DEFAULT_DEV_API_GATEWAY_URL : "")
).replace(/\/+$/, "");
const TOKEN_KEY = "streamsphere_token";

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
  if (!API_GATEWAY_BASE) {
    throw new Error("API gateway is not configured. Set VITE_API_GATEWAY_URL in your deployment environment.");
  }

  // Debug: expose the resolved API base in dev and log each outgoing request
  if (import.meta.env.DEV) {
    try {
      // attach to window for quick inspection in DevTools console
      // @ts-ignore - adding debug field for development only
      window.STREAMSPHERE_API_GATEWAY_BASE = API_GATEWAY_BASE;
      console.debug("[api] request =>", `${API_GATEWAY_BASE}${path}`, {
        method: options.method || "GET",
        headers: options.headers || {},
      });
    } catch (e) {
      // ignore errors attaching to window in non-browser environments
    }
  }

  try {
    const response = await fetch(`${API_GATEWAY_BASE}${path}`, options);

    if (import.meta.env.DEV) {
      // Log response headers to help diagnose CORS / header issues
      try {
        const headersObj: Record<string, string> = {};
        response.headers.forEach((v, k) => (headersObj[k] = v));
        console.debug("[api] response headers <=", `${API_GATEWAY_BASE}${path}`, headersObj, "status", response.status);
      } catch (e) {
        // ignore header iteration errors
      }
    }

    return parseResponse(response);
  } catch (err) {
    // Network or CORS-level failures reach here — log and rethrow for UI handling
    if (import.meta.env.DEV) {
      console.error("[api] request failed =>", `${API_GATEWAY_BASE}${path}`, err);
    }
    throw err;
  }
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
