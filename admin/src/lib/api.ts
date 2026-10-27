import axios from "axios";

const MAX_VIDEO_UPLOAD_BYTES = 100 * 1024 * 1024;

const API_CONFIG_URL =
  import.meta.env.VITE_API_CONFIG_URL || "https://streamsphere-api-gateway.onrender.com";
const TOKEN_KEY = "streamsphere_token";
let apiBaseUrlPromise: Promise<string> | undefined;

export function getApiBaseUrl() {
  if (!apiBaseUrlPromise) {
    apiBaseUrlPromise = axios
      .get(`${API_CONFIG_URL.replace(/\/+$/, "")}/api/config`)
      .then(({ data }) => {
        const apiBaseUrl: unknown = data?.apiBaseUrl;
        if (typeof apiBaseUrl !== "string") {
          throw new Error("Backend API configuration is missing apiBaseUrl");
        }

        const parsedUrl = new URL(apiBaseUrl);
        if (!["http:", "https:"].includes(parsedUrl.protocol)) {
          throw new Error("Backend API configuration must use HTTP or HTTPS");
        }
        return apiBaseUrl.replace(/\/+$/, "");
      })
      .catch((error: unknown) => {
        apiBaseUrlPromise = undefined;
        const message = error instanceof Error ? error.message : String(error);
        throw new Error(`Unable to load backend API configuration: ${message}`);
      });
  }
  return apiBaseUrlPromise;
}

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

async function apiRequest(path, options = {}) {
  try {
    const apiBaseUrl = await getApiBaseUrl();
    const response = await axios.request({
      baseURL: apiBaseUrl,
      url: path,
      method: options.method || "GET",
      headers: options.headers,
      data: options.body,
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

export async function uploadVideo(channelId, videoFile, videoDetails, onUploadProgress) {
  if (!videoFile || typeof videoFile.type !== "string" || !videoFile.type.startsWith("video/")) {
    throw new Error("Choose a valid video file.");
  }
  if (videoFile.size <= 0 || videoFile.size > MAX_VIDEO_UPLOAD_BYTES) {
    throw new Error("Video files must be between 1 byte and 100 MiB (104,857,600 bytes).");
  }

  const authorization = await apiRequest(
    `/api/v1/video/upload/authorization?channelId=${encodeURIComponent(channelId)}&fileName=${encodeURIComponent(videoFile.name)}&fileSize=${videoFile.size}`,
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
      replaceExisting: false,
    }),
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
