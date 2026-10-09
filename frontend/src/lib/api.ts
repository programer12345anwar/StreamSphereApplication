const DEFAULT_DEV_API_GATEWAY_URL = typeof window !== "undefined" ? `${window.location.protocol}//${window.location.hostname}:8080` : "http://localhost:8080";
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

function normalizeVideo(video) {
  if (!video || typeof video !== "object") {
    return video;
  }

  const title = video.title ?? video.name ?? "Untitled video";
  const uploadedAt = video.uploadedAt ?? video.uploadDateTime ?? video.createdAt ?? null;

  return {
    ...video,
    title,
    name: video.name ?? title,
    uploadedAt,
    thumbnailLink: video.thumbnailLink ?? video.thumbnailUrl ?? "",
    thumbnailUrl: video.thumbnailUrl ?? video.thumbnailLink ?? "",
    channelName: video.channelName ?? video.channel?.name ?? "Unknown channel",
    tags: Array.isArray(video.tags) ? video.tags : []
  };
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
  const feed = await apiRequest(`/api/v1/central/videos?limit=${limit}`);
  return Array.isArray(feed) ? feed.map(normalizeVideo) : [];
}

export async function getVideoById(videoId) {
  return normalizeVideo(await apiRequest(`/api/v1/central/videos/${encodeURIComponent(videoId)}`));
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

export async function unsubscribeFromChannel(channelId, userId) {
  return apiRequest(
    `/api/v1/central/channel/${channelId}/unsubscribe?userId=${encodeURIComponent(userId)}`,
    {
      method: "PUT",
      headers: authHeaders(),
    },
  );
}

export async function checkSubscriptionStatus(channelId, userId) {
  return apiRequest(
    `/api/v1/central/channel/${channelId}/check-subscription?userId=${encodeURIComponent(userId)}`,
    {
      headers: authHeaders(),
    },
  );
}

export async function toggleLikeVideo(videoId, userId, isLike) {
  return apiRequest(
    `/api/v1/central/engagement/video/${videoId}/like?userId=${encodeURIComponent(userId)}&isLike=${isLike}`,
    {
      method: "POST",
      headers: authHeaders(),
    },
  );
}

export async function addCommentToVideo(videoId, userId, text, parentId = null) {
  return apiRequest(
    `/api/v1/central/engagement/video/${videoId}/comment?userId=${encodeURIComponent(userId)}`,
    {
      method: "POST",
      headers: authHeaders({ "Content-Type": "application/json" }),
      body: JSON.stringify({ text, parentId }),
    },
  );
}

export async function getVideoComments(videoId, page = 0, size = 20) {
  return apiRequest(`/api/v1/central/engagement/video/${videoId}/comments?page=${page}&size=${size}`);
}

export async function getCommentReplies(parentId, page = 0, size = 20) {
  return apiRequest(`/api/v1/central/engagement/comment/${parentId}/replies?page=${page}&size=${size}`);
}

export async function recordVideoView(videoId) {
  return apiRequest(`/api/v1/central/engagement/video/${videoId}/view`, {
    method: "POST",
    headers: authHeaders(),
  });
}

export async function searchVideos(query, limit = 30) {
  const feed = await apiRequest(`/api/v1/central/videos/search?q=${encodeURIComponent(query)}&limit=${limit}`);
  return Array.isArray(feed) ? feed.map(normalizeVideo) : [];
}

export async function getTrendingFeed(limit = 30) {
  const feed = await apiRequest(`/api/v1/central/videos/trending?limit=${limit}`);
  return Array.isArray(feed) ? feed.map(normalizeVideo) : [];
}

export async function getShorts(limit = 10) {
  const feed = await apiRequest(`/api/v1/central/videos/shorts?limit=${limit}`);
  return Array.isArray(feed) ? feed.map(normalizeVideo) : [];
}

export async function getSubscriptionsFeed(userId, limit = 30) {
  const feed = await apiRequest(`/api/v1/central/videos/subscriptions?userId=${encodeURIComponent(userId)}&limit=${limit}`, {
    headers: authHeaders(),
  });
  return Array.isArray(feed) ? feed.map(normalizeVideo) : [];
}

export async function searchChannels(query) {
  return apiRequest(`/api/v1/central/channel/search?q=${encodeURIComponent(query)}`);
}

export async function getSubscribedChannels(userId) {
  return apiRequest(`/api/v1/central/channel/subscribed?userId=${encodeURIComponent(userId)}`, {
    headers: authHeaders(),
  });
}

export async function getResumeTime(userId, videoId) {
  return apiRequest(`/api/v1/central/library/history/resume?userId=${encodeURIComponent(userId)}&videoId=${encodeURIComponent(videoId)}`, {
    headers: authHeaders(),
  });
}

export async function updateResumeTime(userId, videoId, resumeTime) {
  return apiRequest(
    `/api/v1/central/library/history/resume?userId=${encodeURIComponent(userId)}&videoId=${encodeURIComponent(videoId)}&resumeTime=${Math.floor(resumeTime)}`,
    {
      method: "PUT",
      headers: authHeaders(),
    }
  );
}

export async function reportVideo(userId, videoId, reason) {
  return apiRequest(
    `/api/v1/central/engagement/video/${encodeURIComponent(videoId)}/report?userId=${encodeURIComponent(userId)}`,
    {
      method: "POST",
      headers: authHeaders({ "Content-Type": "application/json" }),
      body: JSON.stringify({ reason }),
    }
  );
}

export async function createPlaylist(channelId, name) {
  return apiRequest(
    `/api/v1/central/library/playlist?channelId=${encodeURIComponent(channelId)}&name=${encodeURIComponent(name)}`,
    {
      method: "POST",
      headers: authHeaders(),
    }
  );
}

export async function getChannelPlaylists(channelId) {
  return apiRequest(`/api/v1/central/library/channel/${encodeURIComponent(channelId)}/playlists`, {
    headers: authHeaders(),
  });
}

export async function addVideoToPlaylist(playlistId, videoId) {
  return apiRequest(
    `/api/v1/central/library/playlist/${encodeURIComponent(playlistId)}/video/${encodeURIComponent(videoId)}`,
    {
      method: "POST",
      headers: authHeaders(),
    }
  );
}

export async function removeVideoFromPlaylist(playlistId, videoId) {
  return apiRequest(
    `/api/v1/central/library/playlist/${encodeURIComponent(playlistId)}/video/${encodeURIComponent(videoId)}`,
    {
      method: "DELETE",
      headers: authHeaders(),
    }
  );
}

export async function deletePlaylist(playlistId) {
  return apiRequest(
    `/api/v1/central/library/playlist/${encodeURIComponent(playlistId)}`,
    {
      method: "DELETE",
      headers: authHeaders(),
    }
  );
}

// Notifications
export async function getNotifications(userId, page = 0, size = 20) {
  return apiRequest(`/api/v1/central/notifications?userId=${encodeURIComponent(userId)}&page=${page}&size=${size}`, {
    headers: authHeaders(),
  });
}

export async function getUnreadNotificationCount(userId) {
  return apiRequest(`/api/v1/central/notifications/unread-count?userId=${encodeURIComponent(userId)}`, {
    headers: authHeaders(),
  });
}

export async function markNotificationAsRead(id) {
  return apiRequest(`/api/v1/central/notifications/${encodeURIComponent(id)}/read`, {
    method: "PUT",
    headers: authHeaders(),
  });
}

export async function markAllNotificationsAsRead(userId) {
  return apiRequest(`/api/v1/central/notifications/read-all?userId=${encodeURIComponent(userId)}`, {
    method: "PUT",
    headers: authHeaders(),
  });
}
