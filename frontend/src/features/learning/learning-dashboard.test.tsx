import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { LearningDashboard } from "@/features/learning/learning-dashboard";

describe("LearningDashboard", () => {
  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it("completes a due ReviewCard and refreshes its immutable history", async () => {
    let completed = false;
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = input.toString();
      if (url === "/api/reviews/due") {
        return response(completed ? [] : [{
          reviewCardId: "card-1",
          title: "Two Sum",
          sourceUrl: "https://leetcode.com/problems/two-sum/",
          difficulty: "EASY",
          recallPrompt: "Welche zwei Zahlen ergeben die Zielsumme?",
          approachNotes: "Bisherige Werte in einer Hashmap merken.",
          reviewMode: "APPROACH_RECALL",
          dueAt: "2026-11-20T08:00:00Z",
        }]);
      }
      if (url === "/api/reviews/history") {
        return response(completed ? [{
          reviewLogId: "log-1",
          reviewCardId: "card-1",
          title: "Two Sum",
          rating: "GOOD",
          reviewedAt: "2026-11-20T10:00:00Z",
          durationMs: 12_000,
          note: "Recalled the complement map.",
          previousDueAt: "2026-11-20T08:00:00Z",
          nextDueAt: "2026-11-23T10:00:00Z",
          schedulerVersion: "phase3-simple-v1",
        }] : []);
      }
      if (url === "/api/provider-connections") {
        return response([]);
      }
      if (url === "/api/providers") {
        return response([{ provider: "CODEWARS", capabilities: [
          "PUBLIC_PROFILE", "MANUAL_IMPORT", "INCREMENTAL_SYNC",
        ] }]);
      }
      if (url === "/api/reviews/card-1/completions" && init?.method === "POST") {
        completed = true;
        return response({ reviewLogId: "log-1" });
      }
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<LearningDashboard />);

    expect(await screen.findByRole("heading", { name: "Two Sum" })).toBeVisible();
    expect(screen.getByText("Welche zwei Zahlen ergeben die Zielsumme?")).toBeVisible();
    expect(screen.queryByText("Bisherige Werte in einer Hashmap merken.")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "GOOD" })).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Zur Originalaufgabe" })).toHaveAttribute(
      "href", "https://leetcode.com/problems/two-sum/",
    );
    fireEvent.click(screen.getByRole("button", { name: "Lösungsansatz aufdecken" }));
    expect(screen.getByText("Bisherige Werte in einer Hashmap merken.")).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Zurück zur Frage" }));
    expect(screen.getByText("Welche zwei Zahlen ergeben die Zielsumme?")).toBeVisible();
    expect(screen.queryByText("Bisherige Werte in einer Hashmap merken.")).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Lösungsansatz aufdecken" }));
    fireEvent.change(screen.getByLabelText("Optionale Review-Notiz"), {
      target: { value: "Recalled the complement map." },
    });
    fireEvent.click(screen.getByRole("button", { name: "GOOD" }));

    expect(await screen.findByText("Recalled the complement map.", { selector: "p" })).toBeVisible();
    expect(screen.getByText("GOOD · 12 Sekunden")).toBeVisible();
    expect(screen.getByText("Heute ist nichts fällig.")).toBeVisible();

    await waitFor(() => {
      const completionCall = fetchMock.mock.calls.find(
        ([url]) => url === "/api/reviews/card-1/completions",
      );
      expect(completionCall).toBeDefined();
      const payload = JSON.parse(String(completionCall?.[1]?.body)) as {
        note: string;
        rating: string;
        startedAt: string;
      };
      expect(payload.note).toBe("Recalled the complement map.");
      expect(payload.rating).toBe("GOOD");
      expect(Date.parse(payload.startedAt)).not.toBeNaN();
    });
  });

  it("connects an unverified Codewars profile and synchronizes completed kata", async () => {
    let connected = false;
    const connection = {
      id: "connection-1",
      provider: "CODEWARS",
      mode: "PUBLIC_PROFILE",
      status: "ACTIVE",
      verificationStatus: "UNVERIFIED",
      externalUserId: "AliceCanonical",
      lastSyncedAt: null,
    };
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = input.toString();
      if (url === "/api/reviews/due" || url === "/api/reviews/history") {
        return response([]);
      }
      if (url === "/api/provider-connections") {
        return response(connected ? [connection] : []);
      }
      if (url === "/api/providers") {
        return response([{ provider: "CODEWARS", capabilities: [
          "PUBLIC_PROFILE", "MANUAL_IMPORT", "INCREMENTAL_SYNC",
        ] }]);
      }
      if (url === "/api/provider-connections/codewars" && init?.method === "POST") {
        connected = true;
        return response(connection, 201);
      }
      if (url === "/api/provider-connections/connection-1/sync-jobs"
          && init?.method === "POST") {
        return response({
          id: "job-1",
          connectionId: "connection-1",
          status: "REQUESTED",
          importedCount: 0,
          existingCount: 0,
          pagesProcessed: 0,
          failureCode: null,
          failureDetail: null,
          retryAfterSeconds: null,
          startedAt: "2026-11-22T10:00:00Z",
          completedAt: null,
        }, 202);
      }
      if (url === "/api/provider-connections/connection-1/sync-jobs/job-1") {
        return response({
          id: "job-1",
          connectionId: "connection-1",
          status: "SUCCEEDED",
          importedCount: 3,
          existingCount: 1,
          pagesProcessed: 1,
          failureCode: null,
          failureDetail: null,
          retryAfterSeconds: null,
          startedAt: "2026-11-21T10:00:00Z",
          completedAt: "2026-11-21T10:00:01Z",
        });
      }
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<LearningDashboard />);

    fireEvent.change(await screen.findByLabelText("Codewars-Benutzername"), {
      target: { value: "alice" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Profil verbinden" }));

    expect(await screen.findByText("AliceCanonical")).toBeVisible();
    expect(screen.getByText("Öffentlich · nicht verifiziert")).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Jetzt synchronisieren" }));

    expect(await screen.findByText("3 neue und 1 bereits bekannte Kata verarbeitet.")).toBeVisible();
    const syncCall = fetchMock.mock.calls.find(
      ([url]) => url === "/api/provider-connections/connection-1/sync-jobs",
    );
    expect(syncCall?.[1]?.headers).toEqual(expect.objectContaining({
      "Idempotency-Key": expect.any(String),
    }));
  });
});

function response(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json" },
  });
}
