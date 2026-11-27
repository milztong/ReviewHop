import { NextRequest } from "next/server";
import { authenticatedBackendRequest, forwardBackendResponse } from "@/lib/backend-api";

export async function GET(request: NextRequest) {
  const response = await authenticatedBackendRequest(request, "/api/v1/providers");
  return response instanceof Response ? forwardBackendResponse(response) : response;
}
