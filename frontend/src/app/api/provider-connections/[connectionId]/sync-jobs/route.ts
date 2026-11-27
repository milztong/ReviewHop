import { NextRequest } from "next/server";
import { authenticatedBackendRequest, forwardBackendResponse } from "@/lib/backend-api";

interface RouteContext {
  params: Promise<{ connectionId: string }>;
}

export async function POST(request: NextRequest, context: RouteContext) {
  const { connectionId } = await context.params;
  const response = await authenticatedBackendRequest(
    request,
    `/api/v1/provider-connections/${encodeURIComponent(connectionId)}/sync-jobs`,
    {
      method: "POST",
      headers: {
        "Idempotency-Key": request.headers.get("Idempotency-Key") ?? "",
      },
    },
  );
  return response instanceof Response ? forwardBackendResponse(response) : response;
}
