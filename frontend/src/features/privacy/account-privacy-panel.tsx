"use client";

import { FormEvent, useState } from "react";
import { signOut } from "next-auth/react";

const DELETE_CONFIRMATION = "LÖSCHEN";

export function AccountPrivacyPanel() {
  const [confirmation, setConfirmation] = useState("");
  const [deleting, setDeleting] = useState(false);
  const [message, setMessage] = useState("");

  async function deleteAccountData(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (confirmation !== DELETE_CONFIRMATION || deleting) return;

    setDeleting(true);
    setMessage("Deine ReviewHop-Daten werden gelöscht …");
    try {
      const response = await fetch("/api/account", {
        method: "DELETE",
        headers: { "X-Account-Deletion-Confirmation": "DELETE" },
      });
      if (!response.ok) {
        setMessage("Die Daten konnten nicht gelöscht werden.");
        return;
      }
      await signOut({ callbackUrl: "/" });
    } catch {
      setMessage("Die Daten konnten nicht gelöscht werden.");
    } finally {
      setDeleting(false);
    }
  }

  return (
    <section className="workspace-card privacy-card" aria-labelledby="privacy-heading">
      <p className="eyebrow">Datenschutz und Portabilität</p>
      <h2 id="privacy-heading">Deine ReviewHop-Daten</h2>
      <p className="provider-notice">
        Der JSON-Export enthält deine Lerninhalte, Reviews und Provider-Verbindungen.
        Interne OIDC-, Outbox- und Trace-Daten werden nicht exportiert.
      </p>
      <a className="secondary-button download-link" href="/api/account/export" download>
        JSON-Export herunterladen
      </a>

      <div className="danger-zone">
        <h3>ReviewHop-Daten dauerhaft löschen</h3>
        <p>
          Dieser Vorgang entfernt deine ReviewHop-Fachdaten und beendet anschließend
          die Sitzung. Dein separater Keycloak-Account wird dadurch nicht gelöscht.
        </p>
        <form className="delete-account-form" onSubmit={deleteAccountData}>
          <label>
            Zur Bestätigung <strong>{DELETE_CONFIRMATION}</strong> eingeben
            <input
              autoComplete="off"
              value={confirmation}
              onChange={(event) => setConfirmation(event.target.value)}
            />
          </label>
          <button
            className="danger-button"
            disabled={confirmation !== DELETE_CONFIRMATION || deleting}
            type="submit"
          >
            {deleting ? "Wird gelöscht …" : "ReviewHop-Daten löschen"}
          </button>
        </form>
        <p className="status-message" role="status">{message}</p>
      </div>
    </section>
  );
}
