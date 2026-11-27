export type ReviewRating = "AGAIN" | "HARD" | "GOOD" | "EASY";

export interface DueReview {
  reviewCardId: string;
  title: string;
  sourceUrl: string;
  difficulty: string | null;
  recallPrompt: string | null;
  approachNotes: string | null;
  reviewMode: "APPROACH_RECALL" | "FULL_RECODE";
  dueAt: string;
}

export interface ReviewHistoryEntry {
  reviewLogId: string;
  reviewCardId: string;
  title: string;
  rating: ReviewRating;
  reviewedAt: string;
  durationMs: number;
  note: string | null;
  previousDueAt: string;
  nextDueAt: string;
  schedulerVersion: string;
}

export interface ProviderConnection {
  id: string;
  provider: "CODEWARS";
  mode: "PUBLIC_PROFILE";
  status: "ACTIVE";
  verificationStatus: "UNVERIFIED";
  externalUserId: string;
  lastSyncedAt: string | null;
}

export interface ProviderDescriptor {
  provider: "CODEWARS";
  capabilities: Array<"PUBLIC_PROFILE" | "MANUAL_IMPORT" | "INCREMENTAL_SYNC">;
}

export interface SyncJob {
  id: string;
  connectionId: string;
  status: "REQUESTED" | "RUNNING" | "RETRY_SCHEDULED" | "SUCCEEDED"
    | "RETRYABLE_FAILED" | "TERMINAL_FAILED" | "DEAD_LETTERED";
  importedCount: number;
  existingCount: number;
  pagesProcessed: number;
  failureCode: string | null;
  failureDetail: string | null;
  retryAfterSeconds: number | null;
  startedAt: string;
  completedAt: string | null;
}
