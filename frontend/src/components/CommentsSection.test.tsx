import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import CommentsSection from "./CommentsSection";
import { addCommentToVideo, getCommentReplies, getVideoComments } from "@/lib/api";

vi.mock("@/contexts/AuthContext", () => ({
  useAuth: () => ({ user: { id: "user-1", name: "Viewer" } }),
}));

vi.mock("@/lib/api", () => ({
  addCommentToVideo: vi.fn(),
  getCommentReplies: vi.fn(),
  getVideoComments: vi.fn(),
}));

vi.mock("@/hooks/use-toast", () => ({ toast: vi.fn() }));

const commentPage = {
  content: [{ id: "comment-1", text: "A thoughtful comment", authorName: "Creator", createdAt: "2025-01-01T00:00:00Z" }],
  totalElements: 1,
  totalPages: 1,
  number: 0,
};

describe("CommentsSection", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getVideoComments).mockResolvedValue(commentPage);
  });

  it("loads a compact comment preview and expands to the full comment controls", async () => {
    render(<CommentsSection videoId="video-1" />);

    expect(await screen.findByText("A thoughtful comment")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Post comment" })).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /comments/i }));
    expect(await screen.findByRole("button", { name: "Post comment" })).toBeInTheDocument();
    expect(getVideoComments).toHaveBeenCalledWith("video-1", 0, 20);
  });

  it("allows retrying replies without collapsing the open thread", async () => {
    vi.mocked(getCommentReplies)
      .mockRejectedValueOnce(new Error("temporary failure"))
      .mockResolvedValueOnce({
        content: [{ id: "reply-1", text: "A reply", authorName: "Viewer" }],
        totalElements: 1,
        totalPages: 1,
        number: 0,
      });

    render(<CommentsSection videoId="video-1" />);
    fireEvent.click(await screen.findByRole("button", { name: /comments/i }));
    fireEvent.click(await screen.findByRole("button", { name: "Reply" }));
    fireEvent.click(await screen.findByRole("button", { name: "Retry" }));

    expect(await screen.findByText("A reply")).toBeInTheDocument();
    expect(getCommentReplies).toHaveBeenCalledTimes(2);
    await waitFor(() => expect(screen.getByRole("button", { name: "Hide replies" })).toHaveAttribute("aria-expanded", "true"));
  });
});
