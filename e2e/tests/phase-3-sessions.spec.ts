import { test, expect } from '@playwright/test';

test.describe('Phase 3: Sessions & Token Management', () => {

  test('User can view and manage their sessions', async ({ page, request }) => {
    // 1. Get the seeded organization ID
    const orgsRes = await request.get('http://localhost:8080/api/v1/organizations');
    const orgs = await orgsRes.json();
    const tenantId = orgs.content[0].id;

    // 2. Login via UI with seeded admin
    await page.goto('/login');
    await page.fill('#tenantId', tenantId);
    await page.fill('#email', 'admin@acme.com');
    await page.fill('#password', 'SecurePassword123!');
    await page.click('button[type="submit"]');

    // Wait for navigation to dashboard
    await expect(page).toHaveURL('/');

    // 3. Navigate to Sessions page
    await page.click('text=Sessions');
    await expect(page).toHaveURL(/\/sessions/);

    // 4. Verify current session is listed
    await expect(page.locator('text=Active Sessions')).toBeVisible();
    
    // We should see at least one active session row (with Revoke button)
    const sessionRows = page.locator('.btn-danger');
    await expect(sessionRows.first()).toBeVisible();

    // 5. Revoke sessions until we are redirected to login
    // Since parallel tests use the same admin account, there might be multiple active sessions.
    // We click Revoke on the first session until the app detects we revoked our own session and kicks us out.
    while (page.url().includes('/sessions')) {
      const revokeBtn = page.locator('.btn-danger').first();
      if (await revokeBtn.isVisible()) {
        await revokeBtn.click();
        await page.waitForTimeout(500); // Wait for the state update or redirect
      } else {
        break;
      }
    }
    
    // Wait for the app to redirect us back to login due to unauthorized
    await expect(page).toHaveURL(/\/login/, { timeout: 10000 });
  });

});
