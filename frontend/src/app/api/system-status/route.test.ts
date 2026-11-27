// @vitest-environment node

import { afterEach, describe, expect, it, vi } from "vitest";
import { GET } from "./route";

describe("GET /api/system-status", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
    vi.unstubAllGlobals();
  });

  it("proxies a healthy backend response", async () => {
    vi.stubEnv("ANKILEET_API_BASE_URL", "http://backend:8080/");
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ application: "reviewhop-api", database: "READY" }), {
        status: 200,
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const response = await GET();

    expect(fetchMock).toHaveBeenCalledWith(
      "http://backend:8080/api/v1/system/status",
      expect.objectContaining({ cache: "no-store" }),
    );
    expect(response.status).toBe(200);
    await expect(response.json()).resolves.toEqual({
      state: "ready",
      status: { application: "reviewhop-api", database: "READY" },
    });
  });

  it("does not expose backend details when the request fails", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("connection refused: secret-host")));

    const response = await GET();

    expect(response.status).toBe(503);
    await expect(response.json()).resolves.toEqual({
      state: "unavailable",
      message: "Die Java-API ist momentan nicht erreichbar.",
    });
  });

  it("rejects an invalid backend contract", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(new Response(JSON.stringify({ database: "MAYBE" }), { status: 200 })),
    );

    const response = await GET();

    expect(response.status).toBe(503);
    await expect(response.json()).resolves.toEqual({
      state: "unavailable",
      message: "Die Java-API ist noch nicht bereit.",
    });
  });
});
