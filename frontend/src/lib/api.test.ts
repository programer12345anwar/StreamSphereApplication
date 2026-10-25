import axios from "axios";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { getVideoById, resolveMediaUrl } from "./api";

vi.mock("axios", () => ({
  default: {
    get: vi.fn(),
    request: vi.fn(),
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
});
