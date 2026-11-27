import AxeBuilder from "@axe-core/playwright";
import { expect, type Page, test } from "@playwright/test";

test("landing page is accessible and does not overflow", async ({ page }) => {
  const firstResponse = await page.goto("/");
  const firstPolicy = firstResponse?.headers()["content-security-policy"] ?? "";
  expect(firstPolicy).toContain("frame-ancestors 'none'");
  expect(firstPolicy).toContain("object-src 'none'");
  expect(firstPolicy).not.toMatch(/unsafe-inline|unsafe-eval/);
  const nonce = firstPolicy.match(/'nonce-([^']+)'/)?.[1];
  expect(nonce).toBeTruthy();
  expect(await page.locator("script[nonce]").first().evaluate((script) =>
    (script as HTMLScriptElement).nonce)).toBe(nonce);
  const secondResponse = await page.reload();
  expect(secondResponse?.headers()["content-security-policy"]).not.toContain(`'nonce-${nonce}'`);
  await expect(page.getByRole("heading", { name: "Gelöst heißt noch nicht behalten." }))
    .toBeVisible();

  await expectNoSeriousAccessibilityViolations(page);
  await expectNoHorizontalOverflow(page);
});

test("authenticated dashboard is accessible and responsive", async ({ page }) => {
  const username = process.env.E2E_ALICE_USERNAME;
  const password = process.env.E2E_ALICE_PASSWORD;
  if (!username || !password) {
    throw new Error("E2E_ALICE_USERNAME and E2E_ALICE_PASSWORD are required");
  }

  await page.goto("/");
  await page.getByRole("button", { name: "Mit OIDC anmelden" }).click();
  await page.locator('input[name="username"]').fill(username);
  await page.locator('input[name="password"]').fill(password);
  await page.locator('button[type="submit"]').click();
  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByRole("heading", { name: "Was willst du dauerhaft behalten?" }))
    .toBeVisible();
  await expect(page.getByRole("status").first()).not.toContainText("werden geladen");

  await expectNoSeriousAccessibilityViolations(page);
  await expectNoHorizontalOverflow(page);
});

async function expectNoSeriousAccessibilityViolations(page: Page) {
  const results = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"])
    .analyze();
  const violations = results.violations.filter((violation) =>
    violation.impact === "critical" || violation.impact === "serious");
  expect(violations).toEqual([]);
}

async function expectNoHorizontalOverflow(page: Page) {
  const overflow = await page.evaluate(() =>
    document.documentElement.scrollWidth - document.documentElement.clientWidth);
  expect(overflow).toBeLessThanOrEqual(1);
}
