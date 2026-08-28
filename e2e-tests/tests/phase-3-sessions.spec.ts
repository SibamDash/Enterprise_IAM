import { test, expect } from '@playwright/test';

test.describe('Phase 3: Sessions & Token Management', () => {

  test('User can view and manage their sessions', async ({ page, request }) => {
    // 1. Setup Tenant and User via API (from Phase 1 & 2 tests)
    const orgRes = await request.post('http://localhost:8080/api/v1/organizations', {
      data: { name: 'Session Test Org', domain: 'sessiontest.com' }
    });
    const org = await orgRes.json();
    const tenantId = org.id;

    await request.post('http://localhost:8080/api/v1/users', {
      headers: { 'X-Tenant-ID': tenantId },
      data: {
        email: 'sessionuser@sessiontest.com',
        firstName: 'Session',
        lastName: 'User',
        role: 'USER',
        password: 'Password123!'
      }
    });

    // 2. Login via UI
    await page.goto('/login');
    await page.fill('#tenantId', tenantId);
    await page.fill('#email', 'sessionuser@sessiontest.com');
    await page.fill('#password', 'Password123!');
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
    await expect(sessionRows).toHaveCount(1);

    // 5. Revoke session
    await sessionRows.first().click();
    
    // After revoking, the list should be empty (since we revoked our own session)
    // Actually, if we revoke our only session, the next API call (or the refresh of the session list) 
    // will fail with 401, which the interceptor will try to refresh.
    // The refresh will fail because the token is revoked, causing a redirect to /login.
    // Let's assert we get redirected to login eventually!
    
    // Wait for the app to redirect us back to login due to unauthorized
    await expect(page).toHaveURL(/\/login/, { timeout: 10000 });
  });

});
