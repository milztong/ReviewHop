export type SystemStatus = {
  application: string;
  database: "READY" | "UNAVAILABLE";
};

export type SystemStatusResult =
  | { state: "ready"; status: SystemStatus }
  | { state: "unavailable"; message: string };
