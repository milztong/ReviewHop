import { expect, type Page, test } from "@playwright/test";
import { resolve } from "node:path";

test("captures the portfolio review flow with synthetic learning items", async ({
  page,
}, testInfo) => {
  const screenshotDir = process.env.ANKILEET_PORTFOLIO_SCREENSHOTS;
  test.skip(!screenshotDir || testInfo.project.name !== "chromium", "Portfolio capture is opt-in");
  if (!screenshotDir) return;

  const demoUser = credentials("ALICE");
  await signIn(page, demoUser.username, demoUser.password);

  const examples = [
    {
      title: "Sort a reading list by priority", slug: "reading-list", tags: "sorting, java",
      prompt: "How do you order books by priority and break ties by title?\n\n"
        + "Example: [(2, B), (1, C), (2, A)] → [(1, C), (2, A), (2, B)].\n"
        + "Constraint: An empty reading list is valid.",
      approach: "Compose the priority and title comparators.",
    },
    {
      title: "Group a week of study sessions", slug: "study-sessions", tags: "collections, java",
      prompt: "How do you collect all the study sessions for each day?",
      approach: "Group sessions by their local calendar date.",
    },
  ];
  for (const example of examples) {
    await page.getByLabel("Titel").fill(example.title);
    await page.getByLabel("HTTPS-Link").fill(`https://example.com/reviewhop-demo/${example.slug}`);
    await page.getByLabel("Aufgabenfrage in eigenen Worten").fill(example.prompt);
    await page.getByLabel("Eigener Lösungsansatz").fill(example.approach);
    await page.getByLabel("Schwierigkeit").fill("EASY");
    await page.getByLabel("Tags, kommasepariert").fill(example.tags);
    await page.getByRole("button", { name: "Aktivieren" }).click();
    await expect(page.getByRole("article", { name: example.title, exact: true })).toBeVisible();
  }

  const due = page.getByRole("region", { name: "Fällige Reviews" });
  await due.screenshot({ path: resolve(screenshotDir, "due-desktop.png"), animations: "disabled" });

  const firstCardTitle = examples[0].title;
  const card = due.getByRole("article", { name: firstCardTitle, exact: true });
  await expect(card.getByText(examples[0].prompt)).toBeVisible();
  await expect(card.getByText(examples[0].approach)).toBeHidden();
  const original = card.getByRole("link", { name: "Zur Originalaufgabe" });
  await expect(original).toHaveAttribute("href", "https://example.com/reviewhop-demo/reading-list");
  await expect(original).toHaveAttribute("target", "_blank");
  await card.getByRole("button", { name: "Lösungsansatz aufdecken" }).press("Enter");
  await expect(card.getByText(examples[0].approach)).toBeVisible();
  await card.getByLabel("Optionale Review-Notiz").fill("Compare priority first, then title.");
  await card.getByRole("button", { name: "GOOD", exact: true }).click();

  const history = page.getByRole("region", { name: "Letzte Reviews" });
  const historyEntry = history.getByRole("listitem").filter({
    has: page.getByText(firstCardTitle, { exact: true }),
  });
  await expect(historyEntry.getByText("Compare priority first, then title.")).toBeVisible();
  await expect(card).toHaveCount(0);

  await page.setViewportSize({ width: 390, height: 844 });
  await historyEntry.screenshot({
    path: resolve(screenshotDir, "history-mobile.png"), animations: "disabled",
  });
});

test("keeps the manual review flow isolated between OIDC users", async ({ browser }) => {
  const alice = credentials("ALICE");
  const bob = credentials("BOB");
  const title = `E2E Recall ${Date.now()}`;
  const sourceUrl = `https://example.com/learning/${Date.now()}`;
  const reviewNote = "E2E approach recalled without hints.";

  const aliceContext = await browser.newContext();
  const alicePage = await aliceContext.newPage();
  await signIn(alicePage, alice.username, alice.password);

  await alicePage.getByLabel("Titel").fill(title);
  await alicePage.getByLabel("HTTPS-Link").fill(sourceUrl);
  await alicePage.getByLabel("Schwierigkeit").fill("EASY");
  await alicePage.getByLabel("Tags, kommasepariert").fill("e2e, recall");
  await alicePage.getByLabel("Eigener Lösungsansatz").fill("Created by the local Phase 3 E2E test.");
  await alicePage.getByRole("button", { name: "Aktivieren" }).click();

  const card = alicePage.getByRole("article", { name: title, exact: true });
  await expect(card.getByText("Die Aufgabenstellung findest du über den Link zur Originalaufgabe."))
    .toBeVisible();
  await card.getByRole("button", { name: "Lösungsansatz aufdecken" }).click();
  await card.getByLabel("Optionale Review-Notiz").fill(reviewNote);
  await card.getByRole("button", { name: "GOOD" }).click();

  const history = alicePage.locator('section[aria-labelledby="history-heading"]');
  const historyEntry = history.getByRole("listitem").filter({ hasText: title });
  await expect(historyEntry.getByText(title, { exact: true })).toBeVisible();
  await expect(historyEntry.getByText(reviewNote, { exact: true })).toBeVisible();
  await aliceContext.close();

  const bobContext = await browser.newContext();
  const bobPage = await bobContext.newPage();
  await signIn(bobPage, bob.username, bob.password);

  const dueReviews = bobPage.locator('section[aria-labelledby="due-heading"]');
  await expect(dueReviews.getByRole("status")).toContainText("Heute ist nichts fällig.");
  await expect(bobPage.getByText(title, { exact: true })).toHaveCount(0);
  await bobContext.close();
});

test("connects and synchronizes an unverified public Codewars profile", async ({ page }) => {
  const alice = credentials("ALICE");
  let connected = false;
  const connection = {
    id: "e2e-codewars-connection",
    provider: "CODEWARS",
    mode: "PUBLIC_PROFILE",
    status: "ACTIVE",
    verificationStatus: "UNVERIFIED",
    externalUserId: "E2ECodewarsProfile",
    lastSyncedAt: null,
  };

  await page.route("**/api/providers", (route) => route.fulfill({
    contentType: "application/json",
    body: JSON.stringify([{
      provider: "CODEWARS",
      capabilities: ["PUBLIC_PROFILE", "MANUAL_IMPORT", "INCREMENTAL_SYNC"],
    }]),
  }));
  await page.route("**/api/provider-connections", (route) => route.fulfill({
    contentType: "application/json",
    body: JSON.stringify(connected ? [connection] : []),
  }));
  await page.route("**/api/provider-connections/codewars", async (route) => {
    connected = true;
    await route.fulfill({ status: 201, contentType: "application/json", body: JSON.stringify(connection) });
  });
  await page.route("**/api/provider-connections/e2e-codewars-connection/sync-jobs", (route) =>
    route.fulfill({
      status: 202,
      contentType: "application/json",
      body: JSON.stringify({
        id: "e2e-sync-job",
        connectionId: connection.id,
        status: "REQUESTED",
        importedCount: 0,
        existingCount: 0,
        pagesProcessed: 0,
        failureCode: null,
        failureDetail: null,
        retryAfterSeconds: null,
        startedAt: "2026-11-22T10:00:00Z",
        completedAt: null,
      }),
    }));
  await page.route(
    "**/api/provider-connections/e2e-codewars-connection/sync-jobs/e2e-sync-job",
    (route) => route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        id: "e2e-sync-job",
        connectionId: connection.id,
        status: "SUCCEEDED",
        importedCount: 3,
        existingCount: 1,
        pagesProcessed: 1,
        failureCode: null,
        failureDetail: null,
        retryAfterSeconds: null,
        startedAt: "2026-11-21T10:00:00Z",
        completedAt: "2026-11-21T10:00:01Z",
      }),
    }),
  );

  await signIn(page, alice.username, alice.password);
  await page.getByLabel("Codewars-Benutzername").fill("e2e-profile");
  await page.getByRole("button", { name: "Profil verbinden" }).click();

  await expect(page.getByText("E2ECodewarsProfile")).toBeVisible();
  await expect(page.getByText("Öffentlich · nicht verifiziert")).toBeVisible();
  await page.getByRole("button", { name: "Jetzt synchronisieren" }).click();
  await expect(page.getByText("3 neue und 1 bereits bekannte Kata verarbeitet.")).toBeVisible();
});

test("exports data and requires explicit confirmation before deletion", async ({ page }) => {
  const alice = credentials("ALICE");
  await signIn(page, alice.username, alice.password);

  await page.route("**/api/account/export", (route) => route.fulfill({
    contentType: "application/json",
    headers: { "content-disposition": "attachment; filename=reviewhop-data-export.json" },
    body: JSON.stringify({ formatVersion: 1 }),
  }));
  const downloadPromise = page.waitForEvent("download");
  await page.getByRole("link", { name: "JSON-Export herunterladen" }).click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toBe("reviewhop-data-export.json");

  let confirmedDeletion = false;
  await page.route("**/api/account", async (route) => {
    confirmedDeletion = route.request().headers()["x-account-deletion-confirmation"] === "DELETE";
    await route.fulfill({ status: 204 });
  });
  const deleteButton = page.getByRole("button", { name: "ReviewHop-Daten löschen" });
  await expect(deleteButton).toBeDisabled();
  await page.getByLabel(/Zur Bestätigung/).fill("LÖSCHEN");
  await expect(deleteButton).toBeEnabled();
  await deleteButton.click();

  await expect(page).toHaveURL(/\/$/);
  expect(confirmedDeletion).toBe(true);
});

async function signIn(page: Page, username: string, password: string) {
  await page.goto("/");
  await page.getByRole("button", { name: "Mit OIDC anmelden" }).click();
  await page.locator('input[name="username"]').fill(username);
  await page.locator('input[name="password"]').fill(password);
  await page.locator('button[type="submit"]').click();
  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByRole("heading", { name: "Was willst du dauerhaft behalten?" }))
    .toBeVisible();
}

function credentials(prefix: "ALICE" | "BOB") {
  const username = process.env[`E2E_${prefix}_USERNAME`];
  const password = process.env[`E2E_${prefix}_PASSWORD`];
  if (!username || !password) {
    throw new Error(`E2E_${prefix}_USERNAME and E2E_${prefix}_PASSWORD are required`);
  }
  return { username, password };
}
