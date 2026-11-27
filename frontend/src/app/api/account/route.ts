import { NextRequest } from "next/server";
import { authenticatedBackendRequest, forwardBackendResponse } from "@/lib/backend-api";

export async function DELETE(request: NextRequest) {
  const confirmation = request.headers.get("x-account-deletion-confirmation");
  if (confirmation !== "DELETE") {
    return Response.json({ title: "Invalid deletion confirmation" }, { status: 400 });
  }
  const response = await authenticatedBackendRequest(
    request,
    "/api/v1/users/me",
    {
      method: "DELETE",
      headers: { "X-Account-Deletion-Confirmation": confirmation },
    },
  );
  return response instanceof Response ? forwardBackendResponse(response) : response;
}
