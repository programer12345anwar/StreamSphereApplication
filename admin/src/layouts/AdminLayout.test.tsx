import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Outlet, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import AdminLayout from "./AdminLayout";

vi.mock("@/lib/adminApi", () => ({
  adminLogout: vi.fn(),
  getStoredUser: () => ({ email: "admin@example.com" }),
}));

describe("AdminLayout mobile navigation", () => {
  beforeEach(() => vi.clearAllMocks());

  it("opens a drawer on mobile and closes it after route selection", () => {
    render(
      <MemoryRouter initialEntries={["/"]}>
        <Routes>
          <Route element={<AdminLayout />}>
            <Route index element={<Outlet />} />
            <Route path="reports" element={<p>Reports content</p>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole("button", { name: "Open admin navigation" }));
    expect(screen.getByRole("complementary", { name: "Mobile admin navigation" })).toHaveAttribute("aria-hidden", "false");
    fireEvent.click(screen.getAllByRole("link", { name: "Reports" })[0]);
    expect(screen.getByText("Reports content")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Open admin navigation" })).toHaveAttribute("aria-expanded", "false");
  });
});
