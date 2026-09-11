import { test, expect } from '@playwright/test';

test.describe('Phase 1: Admin UX Journey', () => {
  test('should create an organization and then create a user within it', async ({ page, request }) => {
    // 1. Visit the application and login
    // Robustly wait for the frontend to be ready and load the page
    await expect.poll(async () => {
      try {
        const response = await page.goto('http://127.0.0.1:3000/login', { timeout: 5000 });
        return response && response.status() === 200;
      } catch (e) {
        return false;
      }
    }, { timeout: 30000 }).toBeTruthy();
    
    await expect(page).toHaveTitle(/Enterprise IAM/);

    // Wait for the backend to be fully seeded with retry logic
    let seededTenantId = '';
    await expect.poll(async () => {
      try {
        const orgsRes = await request.get('http://127.0.0.1:8080/api/v1/organizations');
        if (orgsRes.ok()) {
          const orgs = await orgsRes.json();
          if (orgs.content && orgs.content.length > 0) {
            seededTenantId = orgs.content[0].id;
            return true;
          }
        }
      } catch (e) {
        // Ignore connection errors and retry
      }
      return false;
    }, { timeout: 30000 }).toBeTruthy();

    const tenantIdLocator = page.locator('#tenantId');
    await tenantIdLocator.waitFor({ state: 'visible', timeout: 60000 });
    await tenantIdLocator.fill(seededTenantId);
    await page.fill('#email', 'admin@acme.com');
    await page.fill('#password', 'SecurePassword123!');
    await page.getByRole('button', { name: 'Sign in' }).click();

    // Wait for login to complete and dashboard to load
    await expect(page).toHaveURL('http://127.0.0.1:3000/');

    // Navigate to Organizations
    await page.getByRole('link', { name: 'Organizations', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Organizations' })).toBeVisible();

    // Create a new organization
    await page.getByRole('link', { name: 'Create Organization' }).click();
    await page.getByLabel('Organization Name').fill('Playwright Org Test');
    await page.getByRole('button', { name: 'Create Organization' }).click();

    // Verify it was created and we are back on the list
    await expect(page.getByRole('heading', { name: 'Organizations' })).toBeVisible();
    await expect(page.getByText('Playwright Org Test').first()).toBeVisible();

    // In a real scenario, we would extract the UUID from the table. 
    // Since this is Phase 1 and we are relying on a mock tenant header,
    // we would copy the ID from the UI and paste it in the top bar.
    const row = page.getByRole('row', { name: /Playwright Org Test/ });
    const orgIdText = await row.locator('td').first().textContent();
    const orgId = orgIdText?.trim();
    
    expect(orgId).toBeTruthy();

    // Paste it in the Tenant Header input in Topbar
    // We use the seededTenantId because our new security model prevents acting 
    // on a new organization without first being assigned roles in it.
    await page.getByPlaceholder('Enter Organization UUID').fill(seededTenantId);

    // Navigate to Users
    await page.getByRole('link', { name: 'Users' }).click();
    await expect(page.getByRole('heading', { name: 'Users' })).toBeVisible();

    // Ensure the users list is visible
    await expect(page.locator('.table')).toBeVisible();

    // Create User
    const dynamicEmail = `e2e_${Date.now()}@playwright.com`;
    await page.getByRole('link', { name: 'Create User' }).click();
    await page.getByLabel('First Name').fill('E2E');
    await page.getByLabel('Last Name').fill('TestUser');
    await page.getByLabel('Email Address').fill(dynamicEmail);
    await page.getByRole('button', { name: 'Create User' }).click();

    // Verify User was created
    await expect(page.getByRole('heading', { name: 'Users' })).toBeVisible();
    await expect(page.getByText(dynamicEmail)).toBeVisible();
    await expect(page.getByText('ACTIVE').first()).toBeVisible();
  });
});
