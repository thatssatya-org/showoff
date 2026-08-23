import { afterEach, describe, expect, it, vi } from "vitest";

import { fetchCapabilities, fetchOptionalCapability } from "./capabilities";

describe("public capability client", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("maps a no-content capability response to absence", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 204 })));

    await expect(fetchOptionalCapability("/api/v1/capabilities/music")).resolves.toBeNull();
  });

  it("treats an empty manifest as an empty source list", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 204 })));

    await expect(fetchCapabilities()).resolves.toEqual([]);
  });
});
