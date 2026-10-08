import { describe, it, expect } from "vitest";
import { APP_NAME } from "@/lib/branding";

describe("branding", () => {
  it("uses the production app name", () => {
    expect(APP_NAME).toBe("StreamSphere");
  });
});
