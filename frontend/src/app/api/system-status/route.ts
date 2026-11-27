import { NextResponse } from "next/server";
import type { SystemStatus, SystemStatusResult } from "@/features/system/model/system-status";

const DEFAULT_API_BASE_URL = "http://localhost:8080";
const REQUEST_TIMEOUT_MS = 3_000;

function apiBaseUrl(): string {
  return (process.env.ANKILEET_API_BASE_URL ?? DEFAULT_API_BASE_URL).replace(/\/$/, "");
}

function isSystemStatus(value: unknown): value is SystemStatus {
  if (typeof value !== "object" || value === null) {
    return false;
  }

  const candidate = value as Partial<SystemStatus>;
  return typeof candidate.application === "string"
    && (candidate.database === "READY" || candidate.database === "UNAVAILABLE");
}

export async function GET() {
  try {
    const response = await fetch(`${apiBaseUrl()}/api/v1/system/status`, {
      cache: "no-store",
      signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
    });

    if (!response.ok) {
      return NextResponse.json<SystemStatusResult>(
        { state: "unavailable", message: "Die Java-API meldet einen Fehler." },
        { status: 503 },
      );
    }

    const status: unknown = await response.json();
    if (!isSystemStatus(status) || status.database !== "READY") {
      return NextResponse.json<SystemStatusResult>(
        { state: "unavailable", message: "Die Java-API ist noch nicht bereit." },
        { status: 503 },
      );
    }

    return NextResponse.json<SystemStatusResult>({ state: "ready", status });
  } catch {
    return NextResponse.json<SystemStatusResult>(
      { state: "unavailable", message: "Die Java-API ist momentan nicht erreichbar." },
      { status: 503 },
    );
  }
}
