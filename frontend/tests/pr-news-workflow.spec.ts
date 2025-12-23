import { test, expect } from "@playwright/test";

test("login and view search", async ({ page }) => {
  await page.goto("/login");
  await page.getByRole("button", { name: "Continue" }).click();
  await page.waitForURL("**/dashboard");
  await page.getByRole("link", { name: "Search" }).click();
  await expect(page.getByRole("heading", { name: "News Search" })).toBeVisible();
  await page.getByRole("button", { name: "Search" }).click();
  await page.getByRole("link").first().click();
  await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
  await page.getByRole("link", { name: "Publishing" }).click();
  await expect(page.getByRole("heading", { name: "Publishing" })).toBeVisible();
  await page.getByRole("button", { name: "Publish" }).first().click();
  await page.getByRole("button", { name: "PUBLISHED" }).click();
  await expect(page.getByRole("button", { name: "Unpublish" }).first()).toBeVisible();
});
