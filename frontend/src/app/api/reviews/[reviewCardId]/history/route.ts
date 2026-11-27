import { NextRequest } from "next/server";
import { authenticatedBackendRequest, forwardBackendResponse } from "@/lib/backend-api";

export async function GET(
  request: NextRequest,
  context: { params: Promise<{ reviewCardId: string }> },
) {
  const { reviewCardId } = await context.params;
  const response = await authenticatedBackendRequest(
    request,
    `/api/v1/reviews/${encodeURIComponent(reviewCardId)}/history`,
  );
  return response instanceof Response ? forwardBackendResponse(response) : response;
}
