import { NextRequest } from "next/server";
import { authenticatedBackendRequest, forwardBackendResponse } from "@/lib/backend-api";

export async function POST(request: NextRequest) {
  const response = await authenticatedBackendRequest(
    request,
    "/api/v1/provider-connections/codewars",
    {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: await request.text(),
    },
  );
  return response instanceof Response ? forwardBackendResponse(response) : response;
}
