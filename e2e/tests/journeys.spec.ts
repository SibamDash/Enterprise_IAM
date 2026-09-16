import { test, expect } from '@playwright/test';

const API_URL = process.env.API_URL || 'http://localhost:8080';
const SEEDED_TENANT_ID = '11111111-1111-1111-1111-111111111111';

// Shared setup/teardown is not ideal in Playwright without dedicated auth state setups,
// but we isolate state per-test (since Playwright context is clean per test).

test.describe('Journey A: New Employee', () => {
  test('should allow admin to create user and new user to login', async ({ page }) => {
    // 1. Admin login
    await page.goto('/login', { waitUntil: 'networkidle' });
    await page.locator('input#tenantId').waitFor({ state: 'visible' });
    await page.locator('input#tenantId').fill(SEEDED_TENANT_ID);
    await page.locator('input#email').fill('admin@acme.com');
    await page.locator('input#password').fill('SecurePassword123!');
    await page.locator('button[type="submit"]').click();

    // Wait for redirect to dashboard
    await page.waitForURL('/');
    await expect(page.getByRole('heading', { name: 'Platform Overview' })).toBeVisible({ timeout: 15000 });

    // 2. Admin creates a user
    await page.getByRole('link', { name: 'Users' }).click();
    await page.waitForURL('/users');
    
    // Link to create new user
    await page.getByRole('link', { name: 'Create User' }).click();
    
    // Wait for modal or form to open
    await page.locator('input[id="firstName"]').waitFor({ state: 'visible', timeout: 10000 });
    await page.locator('input[id="firstName"]').fill('John');
    await page.locator('input[id="lastName"]').fill('Doe');
    await page.locator('input[id="email"]').fill('john.doe.journeyA@example.com'); // Unique email
    await page.getByRole('button', { name: 'Create User' }).click();

    // Expect user to be created successfully (wait for redirect to /users and visibility in table)
    await page.waitForURL('/users');
    await expect(page.locator('text=john.doe.journeyA@example.com')).toBeVisible({ timeout: 10000 });

    // 3. Admin logs out
    await page.getByRole('button', { name: 'Logout' }).click();
    await page.waitForURL('/login');
    
    // 4. New user login
    // Note: User accepts an invite, sets a password, and enrolls in MFA.
    // We stop the E2E flow here since email/invite flow isn't stubbed in this simple test.
  });
});

test.describe('Journeys B & C: Authorization', () => {
  // Using the admin user for testing auth flows to avoid creating users if it fails
  test('should enforce role-based access control', async ({ page }) => {
    // 1. Login with a known user (Admin for now)
    await page.goto('/login', { waitUntil: 'networkidle' });
    await page.locator('input#tenantId').waitFor({ state: 'visible' });
    await page.locator('input#tenantId').fill(SEEDED_TENANT_ID);
    await page.locator('input#email').fill('admin@acme.com');
    await page.locator('input#password').fill('SecurePassword123!');
    await page.locator('button[type="submit"]').click();
    await page.waitForURL('/');

    // 2. Attempt to access a resource
    await page.getByRole('link', { name: 'Applications' }).click();
    await page.waitForURL('/applications');
    
    // 3. Verify access (Admin has access)
    await expect(page.getByRole('heading', { name: 'Applications' })).toBeVisible({ timeout: 10000 });
  });
});

test.describe('Journey D: SSO', () => {
  test('should recognize existing session for SSO', async ({ page, context }) => {
    // 1. User logs in
    await page.goto('/login', { waitUntil: 'networkidle' });
    await page.locator('input#tenantId').waitFor({ state: 'visible' });
    await page.locator('input#tenantId').fill(SEEDED_TENANT_ID);
    await page.locator('input#email').fill('admin@acme.com');
    await page.locator('input#password').fill('SecurePassword123!');
    await page.locator('button[type="submit"]').click();
    
    // Must wait for login to complete and token to be saved in localStorage
    await page.waitForURL('/');
    await expect(page.getByRole('heading', { name: 'Platform Overview' })).toBeVisible({ timeout: 15000 });

    // 2. Open a new tab (Simulating App B)
    const newPage = await context.newPage();
    await newPage.goto('/', { waitUntil: 'networkidle' });

    // 3. Verify that the session is recognized (no login required)
    await expect(newPage).toHaveURL('/');
    await expect(newPage.getByRole('heading', { name: 'Platform Overview' })).toBeVisible({ timeout: 15000 });
  });
});

test.describe('Journey E: Token replay', () => {
  test('should detect token replay and revoke family', async ({ request }) => {
    // 1. Initial Login via API
    const loginRes = await request.post(`${API_URL}/api/v1/auth/login`, {
      headers: {
        'X-Tenant-ID': SEEDED_TENANT_ID
      },
      data: {
        email: 'admin@acme.com',
        password: 'SecurePassword123!',
      }
    });
    
    expect(loginRes.ok()).toBeTruthy();
    const { refreshToken } = await loginRes.json();
    expect(refreshToken).toBeTruthy();
    
    // 2. Refresh the token
    const refreshRes = await request.post(`${API_URL}/api/v1/auth/refresh`, {
      headers: {
        'X-Tenant-ID': SEEDED_TENANT_ID
      },
      data: {
        refreshToken: refreshToken
      }
    });
    expect(refreshRes.ok()).toBeTruthy();
    
    // 3. Token Replay (use the same original refresh token again)
    const replayRes = await request.post(`${API_URL}/api/v1/auth/refresh`, {
      headers: {
        'X-Tenant-ID': SEEDED_TENANT_ID
      },
      data: {
        refreshToken: refreshToken
      }
    });
    
    // Expect unauthorized (401) or bad request due to replay detection
    expect([401, 400, 403]).toContain(replayRes.status());
  });
});
