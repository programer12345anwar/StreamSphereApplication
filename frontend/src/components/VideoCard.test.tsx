import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { VideoCard } from "./VideoCard";

describe("VideoCard thumbnails", () => {
  it("uses the branded fallback when a thumbnail URL is missing", () => {
    render(
      <MemoryRouter>
        <VideoCard video={{ id: "video-1", title: "Test video" }} />
      </MemoryRouter>,
    );

    expect(screen.getByRole("img", { name: "Thumbnail for Test video" }))
      .toHaveAttribute("src", "/thumbnail-placeholder.svg");
  });

  it("falls back after a remote thumbnail fails and shows the duration", () => {
    render(
      <MemoryRouter>
        <VideoCard video={{
          id: "video-2",
          title: "Remote thumbnail",
          thumbnailLink: "https://ik.example/thumbnail.jpg",
          durationSeconds: 3661,
        }} />
      </MemoryRouter>,
    );

    const image = screen.getByRole("img", { name: "Thumbnail for Remote thumbnail" });
    expect(image).toHaveAttribute("src", "https://ik.example/thumbnail.jpg");
    fireEvent.error(image);

    expect(image).toHaveAttribute("src", "/thumbnail-placeholder.svg");
    expect(screen.getByText("1:01:01")).toBeInTheDocument();
  });
});
