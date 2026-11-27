import { NextRequest } from "next/server";
import { authenticatedBackendRequest, forwardBackendResponse } from "@/lib/backend-api";

export async function POST(
  request: NextRequest,
  context: { params: Promise<{ reviewCardId: string }> },
) {
  const { reviewCardId } = await context.params;
  const response = await authenticatedBackendRequest(
    request,
    `/api/v1/reviews/${encodeURIComponent(reviewCardId)}/completions`,
    {
      method: "POST",
      headers: {
        "content-type": "application/json",
        "Idempotency-Key": request.headers.get("Idempotency-Key") ?? "",
      },
      body: await request.text(),
    },
  );
  return response instanceof Response ? forwardBackendResponse(response) : response;
}
