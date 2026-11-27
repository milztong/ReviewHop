import { NextRequest } from "next/server";
import { authenticatedBackendRequest, forwardBackendResponse } from "@/lib/backend-api";

interface RouteContext {
  params: Promise<{ connectionId: string; syncJobId: string }>;
}

export async function GET(request: NextRequest, context: RouteContext) {
  const { connectionId, syncJobId } = await context.params;
  const response = await authenticatedBackendRequest(
    request,
    `/api/v1/provider-connections/${encodeURIComponent(connectionId)}`
      + `/sync-jobs/${encodeURIComponent(syncJobId)}`,
  );
  return response instanceof Response ? forwardBackendResponse(response) : response;
}
