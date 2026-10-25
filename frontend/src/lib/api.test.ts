import axios from "axios";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { getVideoById, resolveMediaUrl, uploadVideo } from "./api";

vi.mock("axios", () => ({
  default: {
    get: vi.fn(),
    request: vi.fn(),
    post: vi.fn(),
    isAxiosError: vi.fn(() => false),
  },
}));

describe("video media URLs", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("resolves relative playback and thumbnail links against the configured API gateway", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: { apiBaseUrl: "https://api.example.com/" },
    });
    vi.mocked(axios.request).mockResolvedValue({
      status: 200,
      data: {
        id: "video-1",
        videoLink: "/api/v1/video/stream/video-1.mp4",
        thumbnailLink: "/api/v1/video/stream/video-1_thumb.jpg",
      },
    });

    const video = await getVideoById("video-1");

    expect(video.videoLink).toBe("https://api.example.com/api/v1/video/stream/video-1.mp4");
    expect(video.thumbnailLink).toBe(
      "https://api.example.com/api/v1/video/stream/video-1_thumb.jpg",
    );
    expect(resolveMediaUrl("https://cdn.example.com/video.mp4", "https://api.example.com"))
      .toBe("https://cdn.example.com/video.mp4");
  });

  it("uploads video bytes directly to ImageKit and only then completes metadata persistence", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: { apiBaseUrl: "https://api.example.com" },
    });
    vi.mocked(axios.request)
      .mockResolvedValueOnce({
        status: 200,
        data: {
          publicKey: "public-key",
          videoId: "video-1",
          fileName: "video-1.mp4",
          folder: "/streamsphere/videos/channel-1",
          authentication: { token: "upload-token", expire: "123", signature: "signed" },
        },
      })
      .mockResolvedValueOnce({
        status: 201,
        data: { videoId: "video-1", videoUrl: "https://ik.example/video-1.mp4" },
      });
    vi.mocked(axios.post).mockResolvedValue({
      data: { fileId: "imagekit-file-1" },
    });

    const result = await uploadVideo(
      "channel-1",
      new File(["video"], "clip.mp4", { type: "video/mp4" }),
      { name: "A clip", description: "", tags: [], isShort: false },
    );

    expect(axios.post).toHaveBeenCalledWith(
      "https://upload.imagekit.io/api/v1/files/upload",
      expect.any(FormData),
      expect.objectContaining({ onUploadProgress: undefined }),
    );
    const uploadForm = vi.mocked(axios.post).mock.calls[0][1] as FormData;
    expect(uploadForm.get("fileName")).toBe("video-1.mp4");
    expect(uploadForm.get("folder")).toBe("/streamsphere/videos/channel-1");
    expect(uploadForm.get("signature")).toBe("signed");
    expect(vi.mocked(axios.request).mock.calls[1][0]).toMatchObject({
      url: "/api/v1/video/upload/complete",
      method: "POST",
      data: expect.stringContaining('"fileId":"imagekit-file-1"'),
    });
    expect(result.videoId).toBe("video-1");
  });
});
