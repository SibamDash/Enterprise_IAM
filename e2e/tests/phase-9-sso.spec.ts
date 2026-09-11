import { test, expect } from '@playwright/test';

test.describe('Phase 9: SSO Cross-Application E2E Journey', () => {
  test('should allow user to seamlessly authenticate to multiple applications without re-entering credentials', async ({ page, request }) => {
    // 1. Visit the application and login to establish IAM session
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

    // Login using known admin credentials
    const tenantIdLocator = page.locator('#tenantId');
    await tenantIdLocator.waitFor({ state: 'visible', timeout: 60000 });
    await tenantIdLocator.fill(seededTenantId);
    await page.fill('#email', 'admin@acme.com');
    await page.fill('#password', 'SecurePassword123!');
    await page.getByRole('button', { name: 'Sign in' }).click();

    // Wait for login to complete and dashboard to load
    await expect(page).toHaveURL('http://127.0.0.1:3000/');

    // Retrieve the access token that represents the IAM session
    const accessToken = await page.evaluate(() => localStorage.getItem('accessToken'));
    expect(accessToken).toBeTruthy();

    // 2. Simulate User opening App A (CRM)
    // The CRM app directs the user to the IAM authorize endpoint.
    // The browser will automatically attach the IAM_SESSION HttpOnly cookie.
    const crmAuthUrl = `http://127.0.0.1:8080/oauth2/authorize?response_type=code&client_id=crm-client&scope=openid profile&redirect_uri=http://127.0.0.1:3000/crm/callback`;
    
    await page.goto(crmAuthUrl);

    // Assert that the IAM auto-approves the login and seamlessly redirects back to CRM
    // with an authorization code.
    await expect(page).toHaveURL(/http:\/\/127.0.0.1:3000\/crm\/callback\?code=.+/);

    // 3. Simulate User opening App B (HR)
    // The HR app directs the user to the IAM authorize endpoint.
    const hrAuthUrl = `http://127.0.0.1:8080/oauth2/authorize?response_type=code&client_id=hr-client&scope=openid profile&redirect_uri=http://127.0.0.1:3000/hr/callback`;
    
    await page.goto(hrAuthUrl);

    // Assert that the IAM auto-approves the login and seamlessly redirects back to HR
    // with an authorization code, proving the SSO cross-application experience.
    await expect(page).toHaveURL(/http:\/\/127.0.0.1:3000\/hr\/callback\?code=.+/);
    
    // Explicitly verify no JWT is in the URL (it should only be the code)
    const finalUrl = page.url();
    expect(finalUrl).not.toContain('access_token');
    expect(finalUrl).not.toContain(accessToken!);

    // 4. Verify API requests continue to work with Authorization: Bearer <JWT>
    // This proves the stateless JWT mechanism is preserved for APIs.
    const apiResponse = await request.get('http://127.0.0.1:8080/api/v1/users', {
      headers: {
        'Authorization': `Bearer ${accessToken}`
      }
    });
    expect(apiResponse.ok()).toBeTruthy();

    // 5. Verify Logout invalidates the server-side session
    // Navigate back to the IAM Dashboard and log out
    await page.goto('http://127.0.0.1:3000/');
    await page.getByRole('button', { name: 'Logout' }).click();
    
    // Wait for redirect to login page
    await expect(page).toHaveURL(/.*\/login/);

    // 6. Verify subsequent access to /oauth2/authorize requires authentication again
    // Now that the session is invalidated, attempting SSO should block and ask for login
    await page.goto(crmAuthUrl);
    
    // Should be redirected to the IAM login page instead of auto-approving
    await expect(page).toHaveURL(/.*\/login/);
  });
});
