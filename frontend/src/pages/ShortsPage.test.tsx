import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ShortsPage from "./ShortsPage";
import {
  getShorts,
  getVideoEngagement,
  toggleLikeVideo,
} from "@/lib/api";

class MockIntersectionObserver {
  static latest: MockIntersectionObserver | null = null;
  callback: IntersectionObserverCallback;
  observed: Element[] = [];

  constructor(callback: IntersectionObserverCallback) {
    this.callback = callback;
    MockIntersectionObserver.latest = this;
  }

  observe(target: Element) {
    this.observed.push(target);
  }

  disconnect() {}
  unobserve() {}
  takeRecords() { return []; }

  activate(target: Element) {
    this.callback(
      [{ target, isIntersecting: true, intersectionRatio: 0.9 } as IntersectionObserverEntry],
      this as unknown as IntersectionObserver,
    );
  }
}

vi.mock("@/contexts/AuthContext", () => ({
  useAuth: () => ({ user: { id: "user-1" } }),
}));

vi.mock("@/lib/api", () => ({
  checkSubscriptionStatus: vi.fn().mockResolvedValue(false),
  getShorts: vi.fn(),
  getVideoEngagement: vi.fn(),
  subscribeToChannel: vi.fn(),
  toggleLikeVideo: vi.fn(),
  unsubscribeFromChannel: vi.fn(),
}));

vi.mock("@/components/ReportVideoDialog", () => ({ ReportVideoDialog: () => null }));
vi.mock("@/components/CommentsSection", () => ({ default: () => <div>Short comments</div> }));
vi.mock("@/hooks/use-toast", () => ({ toast: vi.fn() }));

const shortFeed = [
  { id: "short-1", title: "First Short", videoLink: "https://media.example/first.mp4", channelId: "channel-1", channelName: "Creator", description: "First description" },
  { id: "short-2", title: "Second Short", videoLink: "https://media.example/second.mp4", channelId: "channel-2", channelName: "Creator" },
];

describe("ShortsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    MockIntersectionObserver.latest = null;
    vi.stubGlobal("IntersectionObserver", MockIntersectionObserver);
    vi.spyOn(HTMLMediaElement.prototype, "play").mockResolvedValue(undefined);
    vi.spyOn(HTMLMediaElement.prototype, "pause").mockImplementation(() => {});
    vi.mocked(getShorts).mockResolvedValue(shortFeed);
    vi.mocked(getVideoEngagement)
      .mockResolvedValueOnce({ likeCount: 7, commentCount: 3, likedByCurrentUser: false })
      .mockResolvedValue({ likeCount: 8, commentCount: 3, likedByCurrentUser: true });
    vi.mocked(toggleLikeVideo).mockResolvedValue(undefined);
  });

  it("plays only the active Short and automatically activates the next visible item", async () => {
    render(<MemoryRouter><ShortsPage /></MemoryRouter>);

    expect(await screen.findByText("First description")).toBeInTheDocument();
    const players = document.querySelectorAll("video");
    expect(players).toHaveLength(2);
    await waitFor(() => expect(HTMLMediaElement.prototype.play).toHaveBeenCalled());
    expect(HTMLMediaElement.prototype.pause).toHaveBeenCalled();

    const secondShort = document.querySelector('[data-short-id="short-2"]');
    expect(secondShort).not.toBeNull();
    act(() => MockIntersectionObserver.latest?.activate(secondShort!));

    await waitFor(() => expect(screen.getByRole("heading", { name: "Second Short" })).toBeInTheDocument());
    expect(HTMLMediaElement.prototype.play).toHaveBeenCalledTimes(2);
    expect(players[1]).toHaveAttribute("src", "https://media.example/second.mp4");
  });

  it("submits a like and renders the backend-confirmed updated count", async () => {
    render(<MemoryRouter><ShortsPage /></MemoryRouter>);

    expect(await screen.findByText("First description")).toBeInTheDocument();
    fireEvent.click(await screen.findByRole("button", { name: "Like video" }));

    await waitFor(() => expect(toggleLikeVideo).toHaveBeenCalledWith("short-1", "user-1", true));
    expect(await screen.findByRole("button", { name: "Unlike video" })).toBeInTheDocument();
    expect(getVideoEngagement).toHaveBeenCalledWith("short-1");
    expect(screen.getByRole("button", { name: "Unlike video" })).toHaveTextContent("8");
  });
});
