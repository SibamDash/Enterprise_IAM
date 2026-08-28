import { test, expect } from '@playwright/test';

test.describe('Phase 1: Admin UX Journey', () => {
  test('should create an organization and then create a user within it', async ({ page }) => {
    // 1. Visit the application
    await page.goto('http://localhost:3000');
    await expect(page).toHaveTitle(/Vite \+ React/);

    // Navigate to Organizations
    await page.getByRole('link', { name: 'Organizations' }).click();
    await expect(page.getByRole('heading', { name: 'Organizations' })).toBeVisible();

    // Create a new organization
    await page.getByRole('link', { name: 'Create Organization' }).click();
    await page.getByLabel('Organization Name').fill('Playwright Org Test');
    await page.getByRole('button', { name: 'Create Organization' }).click();

    // Verify it was created and we are back on the list
    await expect(page.getByRole('heading', { name: 'Organizations' })).toBeVisible();
    await expect(page.getByText('Playwright Org Test')).toBeVisible();

    // In a real scenario, we would extract the UUID from the table. 
    // Since this is Phase 1 and we are relying on a mock tenant header,
    // we would copy the ID from the UI and paste it in the top bar.
    const row = page.getByRole('row', { name: /Playwright Org Test/ });
    const orgIdText = await row.locator('td').first().textContent();
    const orgId = orgIdText?.trim();
    
    expect(orgId).toBeTruthy();

    // Paste it in the Tenant Header input in Topbar
    await page.getByPlaceholder('Enter Organization UUID').fill(orgId!);

    // Navigate to Users
    await page.getByRole('link', { name: 'Users' }).click();
    await expect(page.getByRole('heading', { name: 'Users' })).toBeVisible();

    // Ensure empty initially
    await expect(page.getByText('No users found in this organization.')).toBeVisible();

    // Create User
    await page.getByRole('link', { name: 'Create User' }).click();
    await page.getByLabel('First Name').fill('E2E');
    await page.getByLabel('Last Name').fill('TestUser');
    await page.getByLabel('Email Address').fill('e2e@playwright.com');
    await page.getByRole('button', { name: 'Create User' }).click();

    // Verify User was created
    await expect(page.getByRole('heading', { name: 'Users' })).toBeVisible();
    await expect(page.getByText('E2E TestUser')).toBeVisible();
    await expect(page.getByText('e2e@playwright.com')).toBeVisible();
    await expect(page.getByText('ACTIVE')).toBeVisible();
  });
});
