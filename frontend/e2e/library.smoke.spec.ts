import { expect, test } from "@playwright/test";

import { installAdminAuth, installGuestAuth, mockPublicApi } from "./support/mockApi";

test.describe("public smoke", () => {
  test.beforeEach(async ({ page }) => {
    await installGuestAuth(page);
    await mockPublicApi(page);
  });

  test("home page renders discovery sections", async ({ page }) => {
    await page.goto("/");

    await expect(page.getByRole("heading", { name: "Discover what is moving through the library this week." })).toBeVisible();
    await expect(page.getByText("Most borrowed this week")).toBeVisible();
    await expect(page.getByText("Books arriving soon")).toBeVisible();
    await expect(page.getByRole("button", { name: "Books" })).toBeVisible();
  });

  test("books workspace can open a detail page", async ({ page }) => {
    await page.goto("/books");

    await expect(page.getByRole("heading", { name: "Search, filter, inspect, and borrow from the library catalog." })).toBeVisible();
    await page.getByRole("button", { name: "View details" }).first().click();

    await expect(page).toHaveURL(/\/books\/1$/);
    await expect(page.getByRole("heading", { name: "Domain-Driven Design" })).toBeVisible();
    await expect(page.getByText("Current fulfillment locations and online access")).toBeVisible();
  });
});

test.describe("operations smoke", () => {
  test.beforeEach(async ({ page }) => {
    await installAdminAuth(page);
    await mockPublicApi(page);
  });

  test("admin workspace renders staff registration controls", async ({ page }) => {
    await page.goto("/admin");

    await expect(page.getByRole("heading", { name: "Task-based operations with a sidebar, dashboard, and focused work surfaces." })).toBeVisible();
    await page.getByRole("button", { name: /Access/ }).click();

    const registrationPanel = page.locator(".admin-panel").filter({
      has: page.getByRole("heading", { name: "Register staff account" }),
    });
    await expect(page.getByRole("heading", { name: "Register staff account" })).toBeVisible();
    await expect(page.getByText("Create the Keycloak identity and local access record together.")).toBeVisible();
    await expect(registrationPanel.getByLabel("Initial password")).toBeVisible();
  });
});
