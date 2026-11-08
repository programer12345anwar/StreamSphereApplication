import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { Header } from "./Header";

vi.mock("@/contexts/AuthContext", () => ({
  useAuth: () => ({
    user: null,
    isAuthenticated: false,
    logout: vi.fn(),
  }),
}));

vi.mock("@/components/NotificationDropdown", () => ({
  NotificationDropdown: () => null,
}));

vi.mock("next-themes", () => ({
  useTheme: () => ({ theme: "system", setTheme: vi.fn() }),
}));

describe("Header mobile navigation", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("opens the mobile navigation and closes it after selecting a route", async () => {
    render(
      <MemoryRouter>
        <Header />
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole("button", { name: "Open navigation menu" }));
    expect(await screen.findByRole("navigation", { name: "Mobile navigation" })).toBeInTheDocument();

    fireEvent.click(screen.getByRole("link", { name: "Shorts" }));
    expect(screen.queryByRole("navigation", { name: "Mobile navigation" })).not.toBeInTheDocument();
  });
});
