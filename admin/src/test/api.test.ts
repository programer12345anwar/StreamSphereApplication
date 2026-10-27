import axios from "axios";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { uploadVideo } from "@/lib/api";

vi.mock("axios", () => ({
  default: {
    get: vi.fn(),
    request: vi.fn(),
    post: vi.fn(),
    isAxiosError: vi.fn(() => false),
  },
}));

describe("admin video upload", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubGlobal("localStorage", {
      getItem: vi.fn(() => "test-token"),
    });
  });

  it("uploads to ImageKit and persists metadata through the completion endpoint", async () => {
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

    const file = Object.assign(new Blob(["video"], { type: "video/mp4" }), {
      name: "clip.mp4",
    });
    const result = await uploadVideo(
      "channel-1",
      file,
      { name: "A clip", description: "", tags: [], isShort: false },
    );

    expect(vi.mocked(axios.request).mock.calls[0][0]).toMatchObject({
      url: expect.stringContaining("/api/v1/video/upload/authorization?"),
      method: "POST",
    });
    expect(axios.post).toHaveBeenCalledWith(
      "https://upload.imagekit.io/api/v1/files/upload",
      expect.any(FormData),
      { onUploadProgress: undefined },
    );
    expect(vi.mocked(axios.request).mock.calls[1][0]).toMatchObject({
      url: "/api/v1/video/upload/complete",
      method: "POST",
      data: expect.stringContaining('"fileId":"imagekit-file-1"'),
    });
    expect(result.videoId).toBe("video-1");
  });

  it("rejects non-video files before requesting upload authorization", async () => {
    await expect(
      uploadVideo("channel-1", { type: "text/plain", size: 10, name: "notes.txt" }, {}),
    ).rejects.toThrow("Choose a valid video file.");
    expect(axios.request).not.toHaveBeenCalled();
  });

  it("rejects files above the ImageKit account limit before requesting authorization", async () => {
    await expect(
      uploadVideo(
        "channel-1",
        { name: "large.mp4", type: "video/mp4", size: 100 * 1024 * 1024 + 1 },
        {},
      ),
    ).rejects.toThrow("100 MiB (104,857,600 bytes)");
    expect(axios.request).not.toHaveBeenCalled();
    expect(axios.post).not.toHaveBeenCalled();
  });
});
