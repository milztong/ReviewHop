import { getToken } from "next-auth/jwt";
import { NextRequest, NextResponse } from "next/server";

const DEFAULT_API_BASE_URL = "http://localhost:8080";
const REQUEST_TIMEOUT_MS = 5_000;

function apiBaseUrl(): string {
  return (process.env.ANKILEET_API_BASE_URL ?? DEFAULT_API_BASE_URL).replace(/\/$/, "");
}

export async function authenticatedBackendRequest(
  request: NextRequest,
  path: string,
  init: RequestInit = {},
): Promise<Response | NextResponse> {
  const secret = process.env.AUTH_SECRET;
  if (!secret) {
    return NextResponse.json(
      { title: "Authentication is not configured" },
      { status: 503 },
    );
  }

  const forwardedProtocol = request.headers.get("x-forwarded-proto");
  const secureCookie = forwardedProtocol === "https" || request.nextUrl.protocol === "https:";
  const token = await getToken({ req: request, secret, secureCookie });
  if (!token?.backendAccessToken) {
    return NextResponse.json({ title: "Unauthorized" }, { status: 401 });
  }
  if (token.backendAccessTokenExpiresAt
      && token.backendAccessTokenExpiresAt <= Math.floor(Date.now() / 1000)) {
    return NextResponse.json(
      { title: "Session expired", detail: "Please sign in again." },
      { status: 401 },
    );
  }

  return fetch(`${apiBaseUrl()}${path}`, {
    ...init,
    cache: "no-store",
    headers: {
      ...init.headers,
      Authorization: `Bearer ${token.backendAccessToken}`,
    },
    signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
  });
}

export async function forwardBackendResponse(response: Response): Promise<NextResponse> {
  const body = await response.text();
  const contentType = response.headers.get("content-type") ?? "application/json";
  const contentDisposition = response.headers.get("content-disposition");
  const headers: Record<string, string> = { "content-type": contentType };
  if (contentDisposition) {
    headers["content-disposition"] = contentDisposition;
  }
  return new NextResponse(body, {
    status: response.status,
    headers,
  });
}
