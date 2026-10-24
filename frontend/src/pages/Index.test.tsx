import { act, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Index from "./Index";
import { getSubscriptionsFeed, getTrendingFeed, getVideoFeed } from "@/lib/api";

vi.mock("@/components/Layout", () => ({
  Layout: ({ children }) => <div>{children}</div>,
}));

vi.mock("@/components/VideoCard", () => ({
  VideoCard: ({ video }) => <article>{video.title}</article>,
}));

vi.mock("@/contexts/AuthContext", () => ({
  useAuth: () => ({ user: null }),
}));

vi.mock("@/lib/api", () => ({
  getVideoFeed: vi.fn(),
  getTrendingFeed: vi.fn(),
  getSubscriptionsFeed: vi.fn(),
}));

describe("Home feed", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("refreshes after upload until the published video appears", async () => {
    vi.mocked(getVideoFeed)
      .mockResolvedValueOnce([])
      .mockResolvedValue([{ id: "uploaded-1", title: "New video" }]);

    render(
      <MemoryRouter
        initialEntries={[{ pathname: "/", state: { uploadedVideoId: "uploaded-1" } }]}
      >
        <Index />
      </MemoryRouter>,
    );

    expect(await screen.findByRole("status")).toHaveTextContent("Your video is processing");

    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 3100));
    });

    expect(await screen.findByText("New video")).toBeInTheDocument();
    expect(getVideoFeed).toHaveBeenCalledTimes(2);
  });
});
