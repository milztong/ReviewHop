"use client";

import { FormEvent, useCallback, useEffect, useRef, useState } from "react";
import type {
  DueReview,
  ProviderConnection,
  ProviderDescriptor,
  ReviewHistoryEntry,
  ReviewRating,
  SyncJob,
} from "@/features/learning/model";
import { RecallCard } from "@/features/learning/recall-card";
import { AccountPrivacyPanel } from "@/features/privacy/account-privacy-panel";

async function fetchDueReviews(): Promise<DueReview[]> {
  const response = await fetch("/api/reviews/due", { cache: "no-store" });
  if (!response.ok) {
    throw new Error(response.status === 401 ? "SESSION_EXPIRED" : "DUE_UNAVAILABLE");
  }
  return response.json() as Promise<DueReview[]>;
}

async function fetchRecentHistory(): Promise<ReviewHistoryEntry[]> {
  const response = await fetch("/api/reviews/history", { cache: "no-store" });
  if (!response.ok) {
    throw new Error(response.status === 401 ? "SESSION_EXPIRED" : "HISTORY_UNAVAILABLE");
  }
  return response.json() as Promise<ReviewHistoryEntry[]>;
}

async function fetchProviderConnections(): Promise<ProviderConnection[]> {
  const response = await fetch("/api/provider-connections", { cache: "no-store" });
  if (!response.ok) {
    throw new Error(response.status === 401 ? "SESSION_EXPIRED" : "CONNECTIONS_UNAVAILABLE");
  }
  return response.json() as Promise<ProviderConnection[]>;
}

async function fetchProviderCatalog(): Promise<ProviderDescriptor[]> {
  const response = await fetch("/api/providers", { cache: "no-store" });
  if (!response.ok) {
    throw new Error(response.status === 401 ? "SESSION_EXPIRED" : "PROVIDERS_UNAVAILABLE");
  }
  return response.json() as Promise<ProviderDescriptor[]>;
}

async function fetchSyncJob(connectionId: string, syncJobId: string): Promise<SyncJob> {
  const response = await fetch(
    `/api/provider-connections/${connectionId}/sync-jobs/${syncJobId}`,
    { cache: "no-store" },
  );
  if (!response.ok) {
    throw new Error("SYNC_STATUS_UNAVAILABLE");
  }
  return response.json() as Promise<SyncJob>;
}

function isActiveSync(job: SyncJob): boolean {
  return ["REQUESTED", "RUNNING", "RETRY_SCHEDULED"].includes(job.status);
}

async function waitForSync(initialJob: SyncJob): Promise<SyncJob> {
  let job = initialJob;
  for (let attempt = 0; attempt < 60 && isActiveSync(job); attempt += 1) {
    if (attempt > 0) {
      await new Promise((resolve) => window.setTimeout(resolve, 1_000));
    }
    job = await fetchSyncJob(job.connectionId, job.id);
  }
  return job;
}

function dueMessage(reviews: DueReview[]): string {
  return reviews.length === 0 ? "Heute ist nichts fällig." : `${reviews.length} Review(s) fällig.`;
}

function formatInstant(value: string): string {
  return new Intl.DateTimeFormat("de-DE", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function formatDuration(durationMs: number): string {
  const seconds = Math.round(durationMs / 1_000);
  if (seconds < 1) return "unter 1 Sekunde";
  if (seconds < 60) return `${seconds} Sekunden`;
  return `${Math.round(seconds / 60)} Minuten`;
}

export function LearningDashboard() {
  const [reviews, setReviews] = useState<DueReview[]>([]);
  const [history, setHistory] = useState<ReviewHistoryEntry[]>([]);
  const [connections, setConnections] = useState<ProviderConnection[]>([]);
  const [codewarsAvailable, setCodewarsAvailable] = useState(false);
  const [notes, setNotes] = useState<Record<string, string>>({});
  const [message, setMessage] = useState("Fällige Reviews werden geladen …");
  const reviewStartedAt = useRef(new Map<string, string>());
  const completionsInFlight = useRef(new Set<string>());
  const [completingCards, setCompletingCards] = useState<Set<string>>(new Set());
  const [syncingConnections, setSyncingConnections] = useState<Set<string>>(new Set());
  const [dashboardLoading, setDashboardLoading] = useState(true);
  const [providerMessage, setProviderMessage] = useState(
    "Codewars-Verbindungen werden geladen …",
  );

  const rememberReviewStarts = useCallback((dueReviews: DueReview[]) => {
    const displayedAt = new Date().toISOString();
    dueReviews.forEach((review) => {
      if (!reviewStartedAt.current.has(review.reviewCardId)) {
        reviewStartedAt.current.set(review.reviewCardId, displayedAt);
      }
    });
  }, []);

  const loadDue = useCallback(async () => {
    const data = await fetchDueReviews();
    rememberReviewStarts(data);
    setReviews(data);
    setMessage(dueMessage(data));
  }, [rememberReviewStarts]);

  const loadHistory = useCallback(async () => {
    setHistory(await fetchRecentHistory());
  }, []);

  useEffect(() => {
    let active = true;
    void Promise.all([
      fetchDueReviews(),
      fetchRecentHistory(),
      fetchProviderConnections(),
      fetchProviderCatalog(),
    ])
      .then(([dueReviews, recentHistory, providerConnections, providers]) => {
        if (!active) return;
        rememberReviewStarts(dueReviews);
        setReviews(dueReviews);
        setHistory(recentHistory);
        setConnections(providerConnections);
        setCodewarsAvailable(providers.some((provider) => provider.provider === "CODEWARS"
          && provider.capabilities.includes("PUBLIC_PROFILE")
          && provider.capabilities.includes("MANUAL_IMPORT")));
        setProviderMessage(providerConnections.length === 0
          ? "Noch kein öffentliches Codewars-Profil verbunden."
          : "Öffentliche Profilzuordnung – die Account-Inhaberschaft ist nicht verifiziert.");
        setMessage(dueMessage(dueReviews));
      })
      .catch((error: unknown) => {
        if (!active) return;
        setMessage(error instanceof Error && error.message === "SESSION_EXPIRED"
          ? "Die Sitzung ist abgelaufen."
          : "Dashboarddaten konnten nicht geladen werden.");
      })
      .finally(() => {
        if (active) setDashboardLoading(false);
      });
    return () => {
      active = false;
    };
  }, [rememberReviewStarts]);

  async function connectCodewars(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    setProviderMessage("Codewars-Profil wird geprüft …");
    try {
      const response = await fetch("/api/provider-connections/codewars", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ username: data.get("username") }),
      });
      if (!response.ok) {
        setProviderMessage(response.status === 404
          ? "Das öffentliche Codewars-Profil wurde nicht gefunden."
          : "Die Codewars-Verbindung konnte nicht erstellt werden.");
        return;
      }
      form.reset();
      setConnections(await fetchProviderConnections());
      setProviderMessage(
        "Codewars-Profil verbunden. Die öffentliche Zuordnung ist nicht verifiziert.",
      );
    } catch {
      setProviderMessage("Die Codewars-Verbindung konnte nicht erstellt werden.");
    }
  }

  async function synchronizeCodewars(connectionId: string) {
    if (syncingConnections.has(connectionId)) return;
    setSyncingConnections((current) => new Set(current).add(connectionId));
    setProviderMessage("Codewars-Abschlüsse werden synchronisiert …");
    try {
      const response = await fetch(`/api/provider-connections/${connectionId}/sync-jobs`, {
        method: "POST",
        headers: { "Idempotency-Key": crypto.randomUUID() },
      });
      if (!response.ok) {
        setProviderMessage("Der Codewars-Sync konnte nicht gestartet werden.");
        return;
      }
      const acceptedJob = await response.json() as SyncJob;
      setProviderMessage("Codewars-Sync wurde angenommen und wird verarbeitet …");
      const job = await waitForSync(acceptedJob);
      if (job.status === "SUCCEEDED") {
        setProviderMessage(
          `${job.importedCount} neue und ${job.existingCount} bereits bekannte Kata verarbeitet.`,
        );
        const [providerConnections] = await Promise.all([
          fetchProviderConnections(),
          loadDue(),
        ]);
        setConnections(providerConnections);
      } else {
        const retry = job.status === "RETRY_SCHEDULED"
          ? " Der Worker versucht es automatisch erneut."
          : job.status === "DEAD_LETTERED"
            ? " Der Auftrag benötigt einen Operator-Retry."
            : "";
        setProviderMessage(isActiveSync(job)
          ? "Der Codewars-Sync läuft weiter. Der Status kann später erneut geladen werden."
          : `${job.failureDetail ?? "Codewars-Sync fehlgeschlagen."}${retry}`);
      }
    } catch {
      setProviderMessage("Der Codewars-Sync konnte nicht abgeschlossen werden.");
    } finally {
      setSyncingConnections((current) => {
        const next = new Set(current);
        next.delete(connectionId);
        return next;
      });
    }
  }

  async function createLearningItem(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    const response = await fetch("/api/learning-items/manual", {
      method: "POST",
      headers: {
        "content-type": "application/json",
        "Idempotency-Key": crypto.randomUUID(),
      },
      body: JSON.stringify({
        title: data.get("title"),
        sourceUrl: data.get("sourceUrl"),
        difficulty: data.get("difficulty") || null,
        tags: String(data.get("tags") ?? "").split(",").map((tag) => tag.trim()).filter(Boolean),
        notes: data.get("notes") || null,
        recallPrompt: data.get("recallPrompt") || null,
        firstCompletedAt: new Date().toISOString(),
      }),
    });
    if (!response.ok) {
      setMessage("Der Lerninhalt konnte nicht gespeichert werden.");
      return;
    }
    form.reset();
    setMessage("Lerninhalt gespeichert und zur Wiederholung aktiviert.");
    await loadDue();
  }

  async function complete(reviewCardId: string, rating: ReviewRating) {
    if (completionsInFlight.current.has(reviewCardId)) return;
    completionsInFlight.current.add(reviewCardId);
    setCompletingCards((current) => new Set(current).add(reviewCardId));

    const startedAt = reviewStartedAt.current.get(reviewCardId) ?? new Date().toISOString();
    try {
      const response = await fetch(`/api/reviews/${reviewCardId}/completions`, {
        method: "POST",
        headers: {
          "content-type": "application/json",
          "Idempotency-Key": crypto.randomUUID(),
        },
        body: JSON.stringify({ rating, startedAt, note: notes[reviewCardId] || null }),
      });
      if (!response.ok) {
        setMessage("Das Review konnte nicht abgeschlossen werden.");
        return;
      }

      reviewStartedAt.current.delete(reviewCardId);
      setNotes((current) => ({ ...current, [reviewCardId]: "" }));
      setMessage(`Review mit ${rating} abgeschlossen.`);
      await Promise.all([loadDue(), loadHistory()]);
    } catch {
      setMessage("Das Review konnte nicht abgeschlossen werden.");
    } finally {
      completionsInFlight.current.delete(reviewCardId);
      setCompletingCards((current) => {
        const next = new Set(current);
        next.delete(reviewCardId);
        return next;
      });
    }
  }

  return (
    <div className="dashboard-grid">
      {codewarsAvailable && <section
        className="workspace-card provider-card"
        aria-labelledby="provider-heading"
        aria-busy={syncingConnections.size > 0}
      >
        <p className="eyebrow">Codewars · öffentliche API</p>
        <h2 id="provider-heading">Gelöste Kata übernehmen</h2>
        <p className="provider-notice">
          ReviewHop liest nur öffentliche Abschluss-Metadaten. Die Verbindung bestätigt nicht,
          dass dir der Codewars-Account gehört.
        </p>
        <form className="provider-form" onSubmit={connectCodewars}>
          <label>
            Codewars-Benutzername
            <input name="username" maxLength={255} required />
          </label>
          <button className="primary-button" type="submit">Profil verbinden</button>
        </form>
        <p className="status-message" role="status">{providerMessage}</p>
        <div className="connection-list">
          {connections.map((connection) => (
            <article key={connection.id}>
              <div>
                <strong>{connection.externalUserId}</strong>
                <span>Öffentlich · nicht verifiziert</span>
                <span>
                  {connection.lastSyncedAt
                    ? `Zuletzt synchronisiert: ${formatInstant(connection.lastSyncedAt)}`
                    : "Noch nicht synchronisiert"}
                </span>
              </div>
              <button
                className="secondary-button"
                disabled={syncingConnections.has(connection.id)}
                onClick={() => synchronizeCodewars(connection.id)}
                type="button"
              >
                {syncingConnections.has(connection.id) ? "Synchronisiert …" : "Jetzt synchronisieren"}
              </button>
            </article>
          ))}
        </div>
      </section>}

      <section className="workspace-card" aria-labelledby="create-heading">
        <p className="eyebrow">Manuelle Quelle</p>
        <h2 id="create-heading">Gelöste Aufgabe hinzufügen</h2>
        <form className="learning-form" onSubmit={createLearningItem}>
          <label>Titel<input name="title" maxLength={200} required /></label>
          <label>HTTPS-Link<input name="sourceUrl" type="url" pattern="https://.*" required /></label>
          <label>
            Aufgabenfrage in eigenen Worten
            <textarea name="recallPrompt" maxLength={4000} rows={3} />
          </label>
          <label>Eigener Lösungsansatz<textarea name="notes" maxLength={4000} rows={3} /></label>
          <label>Schwierigkeit<input name="difficulty" maxLength={50} /></label>
          <label>Tags, kommasepariert<input name="tags" /></label>
          <button className="primary-button" type="submit">Aktivieren</button>
        </form>
      </section>

      <section
        className="workspace-card"
        aria-labelledby="due-heading"
        aria-busy={dashboardLoading || completingCards.size > 0}
      >
        <p className="eyebrow">Approach Recall</p>
        <h2 id="due-heading">Fällige Reviews</h2>
        <p className="status-message" role="status">{message}</p>
        <div className="review-list">
          {reviews.map((review) => (
            <RecallCard
              key={review.reviewCardId}
              review={review}
              note={notes[review.reviewCardId] ?? ""}
              completing={completingCards.has(review.reviewCardId)}
              onNoteChange={(note) => setNotes((current) => ({ ...current, [review.reviewCardId]: note }))}
              onRate={(rating) => complete(review.reviewCardId, rating)}
            />
          ))}
        </div>
      </section>

      <section className="workspace-card history-card" aria-labelledby="history-heading">
        <p className="eyebrow">Unveränderliches ReviewLog</p>
        <h2 id="history-heading">Letzte Reviews</h2>
        {history.length === 0 ? (
          <p className="status-message">Noch keine Reviews abgeschlossen.</p>
        ) : (
          <ol className="history-list">
            {history.map((entry) => (
              <li key={entry.reviewLogId}>
                <div>
                  <strong>{entry.title}</strong>
                  <span>{entry.rating} · {formatDuration(entry.durationMs)}</span>
                </div>
                <div>
                  <time dateTime={entry.reviewedAt}>{formatInstant(entry.reviewedAt)}</time>
                  <span>Nächster Termin: {formatInstant(entry.nextDueAt)}</span>
                </div>
                {entry.note && <p>{entry.note}</p>}
              </li>
            ))}
          </ol>
        )}
      </section>

      <AccountPrivacyPanel />
    </div>
  );
}
