import axios from "axios";

const API_CONFIG_URL =
  import.meta.env.VITE_API_CONFIG_URL || "https://streamsphere-api-gateway.onrender.com";
const TOKEN_KEY = "streamsphere_token";
let apiBaseUrlPromise;

export function getApiBaseUrl() {
  if (!apiBaseUrlPromise) {
    apiBaseUrlPromise = axios
      .get(`${API_CONFIG_URL.replace(/\/+$/, "")}/api/config`)
      .then(({ data }) => {
        const apiBaseUrl = data?.apiBaseUrl;
        if (typeof apiBaseUrl !== "string") {
          throw new Error("Backend API configuration is missing apiBaseUrl");
        }

        const parsedUrl = new URL(apiBaseUrl);
        if (!["http:", "https:"].includes(parsedUrl.protocol)) {
          throw new Error("Backend API configuration must use HTTP or HTTPS");
        }
        return apiBaseUrl.replace(/\/+$/, "");
      })
      .catch((error) => {
        apiBaseUrlPromise = undefined;
        throw new Error(`Unable to load backend API configuration: ${error.message}`);
      });
  }
  return apiBaseUrlPromise;
}

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function requireUserId(userId) {
  if (typeof userId !== "string" || !userId.trim() || userId === "undefined" || userId === "null") {
    throw new Error("Your account profile is incomplete. Sign in again and retry.");
  }
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

export function resolveMediaUrl(mediaUrl, apiBaseUrl) {
  if (typeof mediaUrl !== "string" || !mediaUrl || !mediaUrl.startsWith("/")) {
    return mediaUrl;
  }

  return new URL(mediaUrl, `${apiBaseUrl.replace(/\/+$/, "")}/`).toString();
}

function normalizeVideo(video, apiBaseUrl) {
  if (!video || typeof video !== "object") {
    return video;
  }

  const title = video.title ?? video.name ?? "Untitled video";
  const uploadedAt = video.uploadedAt ?? video.uploadDateTime ?? video.createdAt ?? null;
  const thumbnailLink = resolveMediaUrl(
    video.thumbnailLink ?? video.thumbnailUrl ?? "",
    apiBaseUrl,
  );

  return {
    ...video,
    title,
    name: video.name ?? title,
    uploadedAt,
    videoLink: resolveMediaUrl(video.videoLink, apiBaseUrl),
    thumbnailLink,
    thumbnailUrl: thumbnailLink,
    channelName: video.channelName ?? video.channel?.name ?? "Unknown channel",
    tags: Array.isArray(video.tags) ? video.tags : []
  };
}

async function normalizeVideoFeed(feed) {
  if (!Array.isArray(feed)) {
    return [];
  }

  const apiBaseUrl = await getApiBaseUrl();
  return feed.map((video) => normalizeVideo(video, apiBaseUrl));
}

async function apiRequest(path, options = {}) {
  try {
    const apiBaseUrl = await getApiBaseUrl();
    const response = await axios.request({
      baseURL: apiBaseUrl,
      url: path,
      method: options.method || "GET",
      headers: options.headers,
      data: options.body,
      onUploadProgress: options.onUploadProgress,
    });
    return response.status === 204 ? null : response.data;
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const payload = error.response?.data;
      const message =
        (payload && typeof payload === "object" && (payload.message || payload.error)) ||
        (typeof payload === "string" && payload) ||
        (error.response && `Request failed with status ${error.response.status}`) ||
        error.message;
      throw new Error(message);
    }
    throw error;
  }
}

export function getAuthToken() {
  return getToken();
}

export { TOKEN_KEY };

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

export async function getCurrentUser() {
  return apiRequest("/api/central/user/me", {
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

export async function uploadVideo(
  channelId,
  videoFile,
  videoDetails,
  onUploadProgress,
  replaceVideoId,
) {
  if (!videoFile || typeof videoFile.type !== "string" || !videoFile.type.startsWith("video/")) {
    throw new Error("Choose a valid video file.");
  }
  if (videoFile.size <= 0 || videoFile.size > 500 * 1024 * 1024) {
    throw new Error("Video files must be between 1 byte and 500 MB.");
  }

  const authorization = await apiRequest(
    `/api/v1/video/upload/authorization?channelId=${encodeURIComponent(channelId)}&fileName=${encodeURIComponent(videoFile.name)}&fileSize=${videoFile.size}${replaceVideoId ? `&replaceVideoId=${encodeURIComponent(replaceVideoId)}` : ""}`,
    {
      method: "POST",
      headers: authHeaders(),
    },
  );
  const formData = new FormData();
  formData.append("file", videoFile);
  formData.append("fileName", authorization.fileName);
  formData.append("folder", authorization.folder);
  formData.append("publicKey", authorization.publicKey);
  formData.append("useUniqueFileName", "false");
  Object.entries(authorization.authentication || {}).forEach(([key, value]) => {
    formData.append(key, value);
  });

  let uploadedAsset;
  try {
    const response = await axios.post(
      "https://upload.imagekit.io/api/v1/files/upload",
      formData,
      { onUploadProgress },
    );
    uploadedAsset = response.data;
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const message = error.response?.data?.message || error.message;
      throw new Error(`ImageKit upload failed: ${message}`);
    }
    throw error;
  }

  if (!uploadedAsset?.fileId) {
    throw new Error("ImageKit did not return an uploaded file ID.");
  }

  return apiRequest("/api/v1/video/upload/complete", {
    method: "POST",
    headers: authHeaders({ "Content-Type": "application/json" }),
    body: JSON.stringify({
      ...videoDetails,
      channelId,
      videoId: authorization.videoId,
      fileId: uploadedAsset.fileId,
      replaceExisting: Boolean(authorization.replacement),
    }),
  });
}

export function isLegacyLocalMediaUrl(mediaUrl) {
  if (typeof mediaUrl !== "string" || !mediaUrl) {
    return false;
  }
  try {
    return new URL(mediaUrl, window.location.origin).pathname.startsWith(
      "/api/v1/video/stream/",
    );
  } catch {
    return false;
  }
}

export async function getVideoFeed(limit = 30) {
  const feed = await apiRequest(`/api/v1/central/videos?limit=${limit}`);
  return normalizeVideoFeed(feed);
}

export async function getVideoById(videoId) {
  const video = await apiRequest(`/api/v1/central/videos/${encodeURIComponent(videoId)}`);
  const apiBaseUrl = await getApiBaseUrl();
  return normalizeVideo(video, apiBaseUrl);
}

export async function subscribeToChannel(channelId, userId) {
  requireUserId(userId);
  return apiRequest(
    `/api/v1/central/channel/${channelId}/subscribe?userId=${encodeURIComponent(userId)}`,
    {
      method: "PUT",
      headers: authHeaders(),
    },
  );
}

export async function unsubscribeFromChannel(channelId, userId) {
  requireUserId(userId);
  return apiRequest(
    `/api/v1/central/channel/${channelId}/unsubscribe?userId=${encodeURIComponent(userId)}`,
    {
      method: "PUT",
      headers: authHeaders(),
    },
  );
}

export async function checkSubscriptionStatus(channelId, userId) {
  requireUserId(userId);
  return apiRequest(
    `/api/v1/central/channel/${channelId}/check-subscription?userId=${encodeURIComponent(userId)}`,
    {
      headers: authHeaders(),
    },
  );
}

export async function toggleLikeVideo(videoId, userId, isLike) {
  requireUserId(userId);
  return apiRequest(
    `/api/v1/central/engagement/video/${videoId}/like?userId=${encodeURIComponent(userId)}&isLike=${isLike}`,
    {
      method: "POST",
      headers: authHeaders(),
    },
  );
}

export async function addCommentToVideo(videoId, userId, text, parentId = null) {
  requireUserId(userId);
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
  return normalizeVideoFeed(feed);
}

export async function getTrendingFeed(limit = 30) {
  const feed = await apiRequest(`/api/v1/central/videos/trending?limit=${limit}`);
  return normalizeVideoFeed(feed);
}

export async function getShorts(limit = 10) {
  const feed = await apiRequest(`/api/v1/central/videos/shorts?limit=${limit}`);
  return normalizeVideoFeed(feed);
}

export async function getSubscriptionsFeed(userId, limit = 30) {
  requireUserId(userId);
  const feed = await apiRequest(`/api/v1/central/videos/subscriptions?userId=${encodeURIComponent(userId)}&limit=${limit}`, {
    headers: authHeaders(),
  });
  return normalizeVideoFeed(feed);
}

export async function searchChannels(query) {
  return apiRequest(`/api/v1/central/channel/search?q=${encodeURIComponent(query)}`);
}

export async function getSubscribedChannels(userId) {
  requireUserId(userId);
  return apiRequest(`/api/v1/central/channel/subscribed?userId=${encodeURIComponent(userId)}`, {
    headers: authHeaders(),
  });
}

export async function getResumeTime(userId, videoId) {
  requireUserId(userId);
  return apiRequest(`/api/v1/central/library/history/resume?userId=${encodeURIComponent(userId)}&videoId=${encodeURIComponent(videoId)}`, {
    headers: authHeaders(),
  });
}

export async function updateResumeTime(userId, videoId, resumeTime) {
  requireUserId(userId);
  return apiRequest(
    `/api/v1/central/library/history/resume?userId=${encodeURIComponent(userId)}&videoId=${encodeURIComponent(videoId)}&resumeTime=${Math.floor(resumeTime)}`,
    {
      method: "PUT",
      headers: authHeaders(),
    }
  );
}

export async function reportVideo(userId, videoId, reason) {
  requireUserId(userId);
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
  requireUserId(userId);
  return apiRequest(`/api/v1/central/notifications?userId=${encodeURIComponent(userId)}&page=${page}&size=${size}`, {
    headers: authHeaders(),
  });
}

export async function getUnreadNotificationCount(userId) {
  requireUserId(userId);
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
  requireUserId(userId);
  return apiRequest(`/api/v1/central/notifications/read-all?userId=${encodeURIComponent(userId)}`, {
    method: "PUT",
    headers: authHeaders(),
  });
}
