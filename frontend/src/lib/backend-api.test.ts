import { afterEach, describe, expect, it, vi } from "vitest";
import { getToken } from "next-auth/jwt";
import { NextRequest } from "next/server";
import { authenticatedBackendRequest, forwardBackendResponse } from "@/lib/backend-api";

vi.mock("next-auth/jwt", () => ({ getToken: vi.fn() }));

const mockedGetToken = vi.mocked(getToken);

describe("authenticatedBackendRequest", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    delete process.env.AUTH_SECRET;
  });

  it("does not call the backend without an authenticated BFF token", async () => {
    process.env.AUTH_SECRET = "test-only-secret-with-at-least-32-bytes";
    mockedGetToken.mockResolvedValue(null);
    const fetchSpy = vi.spyOn(globalThis, "fetch");

    const response = await authenticatedBackendRequest(
      new NextRequest("http://localhost:3000/api/reviews/due"),
      "/api/v1/reviews/due",
    );

    expect(response.status).toBe(401);
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it("reads the secure production cookie and forwards the access token only server-side", async () => {
    process.env.AUTH_SECRET = "test-only-secret-with-at-least-32-bytes";
    mockedGetToken.mockResolvedValue({
      backendAccessToken: "backend-token",
      backendAccessTokenExpiresAt: Math.floor(Date.now() / 1000) + 60,
    });
    const fetchSpy = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response("[]"));
    const request = new NextRequest("http://frontend:3000/api/reviews/due", {
      headers: { "x-forwarded-proto": "https" },
    });

    await authenticatedBackendRequest(request, "/api/v1/reviews/due");

    expect(mockedGetToken).toHaveBeenCalledWith(expect.objectContaining({
      req: request,
      secret: process.env.AUTH_SECRET,
      secureCookie: true,
    }));
    expect(fetchSpy).toHaveBeenCalledWith(
      "http://localhost:8080/api/v1/reviews/due",
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer backend-token" }),
      }),
    );
  });

  it("forwards the safe download headers but no other backend headers", async () => {
    const response = await forwardBackendResponse(new Response("{}", {
      headers: {
        "content-type": "application/json",
        "content-disposition": "attachment; filename=reviewhop-data-export.json",
        "x-internal-header": "must-not-leak",
      },
    }));

    expect(response.headers.get("content-disposition"))
      .toBe("attachment; filename=reviewhop-data-export.json");
    expect(response.headers.get("x-internal-header")).toBeNull();
  });
});
