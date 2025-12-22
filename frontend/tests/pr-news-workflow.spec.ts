import { test, expect } from "@playwright/test";

test("login and view search", async ({ page }) => {
  await page.goto("/login");
  await page.getByRole("button", { name: "Continue" }).click();
  await page.waitForURL("**/dashboard");
  await page.getByRole("link", { name: "Search" }).click();
  await expect(page.getByRole("heading", { name: "News Search" })).toBeVisible();
});
