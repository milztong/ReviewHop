import { randomBytes } from "node:crypto";
import { type NextRequest, NextResponse } from "next/server";

export function proxy(request: NextRequest) {
  const nonce = randomBytes(16).toString("base64");
  const isDevelopment = process.env.NODE_ENV === "development";
  const issuer = process.env.AUTH_KEYCLOAK_ISSUER;
  const formOrigins = issuer ? `'self' ${new URL(issuer).origin}` : "'self'";
  const policy = [
    "default-src 'self'",
    `script-src 'self' 'nonce-${nonce}' 'strict-dynamic'${isDevelopment ? " 'unsafe-eval'" : ""}`,
    isDevelopment ? "style-src 'self' 'unsafe-inline'" : `style-src 'self' 'nonce-${nonce}'`,
    "img-src 'self' data:",
    "base-uri 'none'",
    "object-src 'none'",
    "frame-ancestors 'none'",
    `form-action ${formOrigins}`,
  ].join("; ");

  const requestHeaders = new Headers(request.headers);
  requestHeaders.set("Content-Security-Policy", policy);
  const response = NextResponse.next({ request: { headers: requestHeaders } });
  response.headers.set("Content-Security-Policy", policy);
  return response;
}

export const config = {
  matcher: ["/((?!api/|_next/|favicon.ico).*)"],
};
