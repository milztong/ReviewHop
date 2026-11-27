"use client";

import { useEffect, useState } from "react";
import type { SystemStatusResult } from "@/features/system/model/system-status";

type ViewState =
  | { state: "loading" }
  | SystemStatusResult;

const INITIAL_STATE: ViewState = { state: "loading" };

export function SystemStatusCard() {
  const [viewState, setViewState] = useState<ViewState>(INITIAL_STATE);

  useEffect(() => {
    const controller = new AbortController();

    async function loadStatus() {
      try {
        const response = await fetch("/api/system-status", {
          cache: "no-store",
          signal: controller.signal,
        });
        const result = (await response.json()) as SystemStatusResult;
        setViewState(result);
      } catch (error) {
        if (error instanceof DOMException && error.name === "AbortError") {
          return;
        }
        setViewState({
          state: "unavailable",
          message: "Der Systemstatus konnte nicht geladen werden.",
        });
      }
    }

    void loadStatus();
    return () => controller.abort();
  }, []);

  const ready = viewState.state === "ready";

  return (
    <section className="status-card" aria-live="polite" aria-busy={viewState.state === "loading"}>
      <div className="status-card__header">
        <p className="eyebrow">Walking Skeleton</p>
        <span className={`status-dot status-dot--${ready ? "ready" : viewState.state}`} aria-hidden="true" />
      </div>

      {viewState.state === "loading" && (
        <>
          <h2>Verbindung wird geprüft</h2>
          <p>Next.js fragt die Java-API und deren PostgreSQL-Verbindung ab.</p>
        </>
      )}

      {viewState.state === "ready" && (
        <>
          <h2>Der technische Pfad steht</h2>
          <p>
            Web, API, Flyway und PostgreSQL antworten als ein zusammenhängender Flow.
          </p>
          <dl className="status-list">
            <div>
              <dt>Backend</dt>
              <dd>{viewState.status.application}</dd>
            </div>
            <div>
              <dt>Datenbank</dt>
              <dd>{viewState.status.database}</dd>
            </div>
          </dl>
        </>
      )}

      {viewState.state === "unavailable" && (
        <>
          <h2>Backend noch nicht erreichbar</h2>
          <p>{viewState.message}</p>
          <p className="status-card__hint">Starte den Stack mit docker compose up --build.</p>
        </>
      )}
    </section>
  );
}
