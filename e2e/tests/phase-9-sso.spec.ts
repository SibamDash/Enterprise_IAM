import { test, expect } from '@playwright/test';

test.describe('Phase 9: SSO Cross-Application E2E Journey', () => {
  test('should allow user to seamlessly authenticate to multiple applications without re-entering credentials', async ({ page, request }) => {
    // 1. Visit the application and login to establish IAM session
    await page.goto('http://localhost:3000/login');
    await expect(page).toHaveTitle(/Enterprise IAM/);

    // Get the seeded organization ID
    const orgsRes = await request.get('http://localhost:8080/api/v1/organizations');
    const orgs = await orgsRes.json();
    const seededTenantId = orgs.content[0].id;

    // Login using known admin credentials
    await page.getByLabel('Organization ID (Tenant)').fill(seededTenantId);
    await page.getByLabel('Email address').fill('admin@acme.com');
    await page.getByLabel('Password').fill('SecurePassword123!');
    await page.getByRole('button', { name: 'Sign in' }).click();

    // Wait for login to complete and dashboard to load
    await expect(page).toHaveURL('http://localhost:3000/');

    // Retrieve the access token that represents the IAM session
    const accessToken = await page.evaluate(() => localStorage.getItem('accessToken'));
    expect(accessToken).toBeTruthy();

    // 2. Simulate User opening App A (CRM)
    // The CRM app directs the user to the IAM authorize endpoint.
    // The frontend intercepts the redirect and appends the access_token.
    const crmAuthUrl = `http://localhost:8080/oauth2/authorize?response_type=code&client_id=crm-client&scope=openid profile&redirect_uri=http://127.0.0.1:3000/crm/callback&access_token=${accessToken}`;
    
    await page.goto(crmAuthUrl);

    // Assert that the IAM auto-approves the login and seamlessly redirects back to CRM
    // with an authorization code.
    await expect(page).toHaveURL(/http:\/\/127\.0\.0\.1:3000\/crm\/callback\?code=.+/);

    // 3. Simulate User opening App B (HR)
    // The HR app directs the user to the IAM authorize endpoint.
    const hrAuthUrl = `http://localhost:8080/oauth2/authorize?response_type=code&client_id=hr-client&scope=openid profile&redirect_uri=http://127.0.0.1:3000/hr/callback&access_token=${accessToken}`;
    
    await page.goto(hrAuthUrl);

    // Assert that the IAM auto-approves the login and seamlessly redirects back to HR
    // with an authorization code, proving the SSO cross-application experience.
    await expect(page).toHaveURL(/http:\/\/127\.0\.0\.1:3000\/hr\/callback\?code=.+/);
  });
});
