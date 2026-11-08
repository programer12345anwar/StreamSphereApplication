import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import AdminReportsPage from "./AdminReportsPage";
import { request } from "@/lib/adminApi";

vi.mock("@/lib/adminApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/adminApi")>();
  return { ...actual, request: vi.fn() };
});

const page = {
  content: [{
    id: 21,
    videoId: "video-1",
    videoName: "Reported video",
    videoLink: "https://media.example/video.mp4",
    thumbnailUrl: "/thumbnail-placeholder.svg",
    reporterName: "Viewer",
    reporterEmail: "viewer@example.com",
    reason: "Spam or misleading",
    description: "Repeated links",
    status: "PENDING",
    createdAt: "2026-11-08T10:00:00Z",
  }],
  number: 0,
  totalPages: 1,
  totalElements: 1,
  first: true,
  last: true,
};

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={client}><AdminReportsPage /></QueryClientProvider>);
}

describe("AdminReportsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(request).mockResolvedValue(page);
  });

  it("loads real report data and persists status changes", async () => {
    renderPage();

    expect((await screen.findAllByText("Reported video")).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Spam or misleading/).length).toBeGreaterThan(0);
    fireEvent.click(screen.getAllByRole("button", { name: "Mark reviewed" })[0]);

    await waitFor(() => expect(request).toHaveBeenCalledWith(
      "/api/v1/admin/reports/21/status?status=REVIEWED",
      { method: "PUT" },
    ));
  });

  it("distinguishes an empty result from an API failure", async () => {
    vi.mocked(request).mockResolvedValue({ ...page, content: [], totalElements: 0 });
    renderPage();

    expect(await screen.findByText("No reports have been submitted yet.")).toBeInTheDocument();
  });
});
