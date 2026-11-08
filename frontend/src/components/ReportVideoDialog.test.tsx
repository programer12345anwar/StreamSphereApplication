import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ReportVideoDialog } from "./ReportVideoDialog";
import { reportVideo } from "@/lib/api";
import { toast } from "@/hooks/use-toast";

vi.mock("@/lib/api", () => ({ reportVideo: vi.fn() }));
vi.mock("@/hooks/use-toast", () => ({ toast: vi.fn() }));

describe("ReportVideoDialog", () => {
  const onOpenChange = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("only confirms submission after the API returns a persisted pending report", async () => {
    vi.mocked(reportVideo).mockResolvedValue({ id: 21, status: "PENDING" });
    render(<ReportVideoDialog videoId="video-1" open onOpenChange={onOpenChange} />);

    fireEvent.change(screen.getByLabelText("Reason"), { target: { value: "Spam or misleading" } });
    fireEvent.change(screen.getByPlaceholderText(/add context/i), { target: { value: "Repeated links" } });
    fireEvent.click(screen.getByRole("button", { name: "Submit report" }));

    await waitFor(() => expect(reportVideo).toHaveBeenCalledWith(
      "video-1",
      "Spam or misleading",
      "Repeated links",
    ));
    expect(toast).toHaveBeenCalledWith(expect.objectContaining({ title: "Report submitted" }));
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it("does not claim success when submission fails", async () => {
    vi.mocked(reportVideo).mockRejectedValue(new Error("Service unavailable"));
    render(<ReportVideoDialog videoId="video-1" open onOpenChange={onOpenChange} />);

    fireEvent.change(screen.getByLabelText("Reason"), { target: { value: "Spam or misleading" } });
    fireEvent.click(screen.getByRole("button", { name: "Submit report" }));

    await waitFor(() => expect(toast).toHaveBeenCalledWith(expect.objectContaining({
      title: "Could not submit report",
      description: "Service unavailable",
    })));
    expect(onOpenChange).not.toHaveBeenCalledWith(false);
  });
});
