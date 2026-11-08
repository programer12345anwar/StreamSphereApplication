import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import WatchPage from "./WatchPage";
import { getVideoById, getVideoFeed } from "@/lib/api";

vi.mock("@/contexts/AuthContext", () => ({
  useAuth: () => ({ user: null }),
}));

vi.mock("@/lib/api", () => ({
  checkSubscriptionStatus: vi.fn(),
  subscribeToChannel: vi.fn(),
  unsubscribeFromChannel: vi.fn(),
  toggleLikeVideo: vi.fn(),
  recordVideoView: vi.fn().mockResolvedValue(undefined),
  getVideoById: vi.fn(),
  getVideoFeed: vi.fn(),
  getMyChannel: vi.fn(),
  getResumeTime: vi.fn(),
  updateResumeTime: vi.fn(),
  isLegacyLocalMediaUrl: vi.fn().mockReturnValue(false),
  reportVideo: vi.fn(),
}));

vi.mock("@/components/Layout", () => ({
  Layout: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

vi.mock("@/components/CommentsSection", () => ({ default: () => null }));
vi.mock("@/components/PlaylistModal", () => ({ PlaylistModal: () => null }));
vi.mock("@/hooks/use-toast", () => ({ toast: vi.fn() }));

const videos = [
  {
    id: "video-1",
    title: "Video one",
    channelName: "Creator",
    videoLink: "https://media.example/video-1.mp4",
    thumbnailLink: "/thumbnail-placeholder.svg",
    views: 10,
  },
  {
    id: "video-2",
    title: "Video two",
    channelName: "Creator",
    videoLink: "https://media.example/video-2.mp4",
    thumbnailLink: "/thumbnail-placeholder.svg",
    views: 20,
  },
];

describe("WatchPage playback queue", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getVideoById).mockImplementation(async (id) => videos.find((video) => video.id === id));
    vi.mocked(getVideoFeed).mockResolvedValue(videos);
  });

  it("keeps the player element mounted while selecting the next queued video", async () => {
    render(
      <MemoryRouter initialEntries={["/watch/video-1"]}>
        <Routes>
          <Route path="/watch/:videoId" element={<WatchPage />} />
        </Routes>
      </MemoryRouter>,
    );

    expect(await screen.findByRole("heading", { name: "Video one" })).toBeInTheDocument();
    const player = document.querySelector("video");
    expect(player).not.toBeNull();
    expect(player?.closest(".sticky")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Open playback queue with 2 videos" }));
    const queue = await screen.findByRole("region", { name: "Up next videos" });
    fireEvent.click(queue.querySelector('a[href="/watch/video-2"]')!);

    expect(await screen.findByRole("heading", { name: "Video two" })).toBeInTheDocument();
    await waitFor(() => expect(document.querySelector("video")).toBe(player));
    expect(player).toHaveAttribute("src", "https://media.example/video-2.mp4");
  });
});
