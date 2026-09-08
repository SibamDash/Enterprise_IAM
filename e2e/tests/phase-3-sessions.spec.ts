import { test, expect } from '@playwright/test';

test.describe('Phase 3: Sessions & Token Management', () => {

  test('User can view and manage their sessions', async ({ page, request }) => {
    // Wait for the backend to be fully seeded with retry logic
    let tenantId = '';
    await expect.poll(async () => {
      try {
        const orgsRes = await request.get('http://localhost:8080/api/v1/organizations');
        if (orgsRes.ok()) {
          const orgs = await orgsRes.json();
          if (orgs.content && orgs.content.length > 0) {
            tenantId = orgs.content[0].id;
            return true;
          }
        }
      } catch (e) {
        // Ignore connection errors and retry
      }
      return false;
    }, { timeout: 30000 }).toBeTruthy();

    // 2. Login via UI with seeded admin
    await expect.poll(async () => {
      try {
        const response = await page.goto('http://localhost:3000/login', { timeout: 5000 });
        return response && response.status() === 200;
      } catch (e) {
        return false;
      }
    }, { timeout: 30000 }).toBeTruthy();
    
    const tenantIdLocator = page.locator('#tenantId');
    await tenantIdLocator.waitFor({ state: 'visible', timeout: 15000 });
    await tenantIdLocator.fill(tenantId);
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
