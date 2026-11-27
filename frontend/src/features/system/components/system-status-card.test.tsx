import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { SystemStatusCard } from "./system-status-card";

describe("SystemStatusCard", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("shows the ready state returned by the BFF route", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        json: async () => ({
          state: "ready",
          status: { application: "reviewhop-api", database: "READY" },
        }),
      }),
    );

    render(<SystemStatusCard />);

    expect(await screen.findByRole("heading", { name: "Der technische Pfad steht" })).toBeVisible();
    expect(screen.getByText("reviewhop-api")).toBeVisible();
    expect(screen.getByText("READY")).toBeVisible();
  });

  it("shows an actionable message when the request fails", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("network error")));

    render(<SystemStatusCard />);

    expect(await screen.findByRole("heading", { name: "Backend noch nicht erreichbar" })).toBeVisible();
    expect(screen.getByText(/docker compose up --build/i)).toBeVisible();
  });
});
