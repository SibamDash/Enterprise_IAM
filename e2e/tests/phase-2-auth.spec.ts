import { test, expect } from '@playwright/test';

test.describe('Phase 2: Authentication', () => {
  test('should allow user to navigate to login and see errors for invalid credentials', async ({ page, request }) => {
    // Wait for the backend to be fully seeded with retry logic
    await expect.poll(async () => {
      try {
        const orgsRes = await request.get('http://127.0.0.1:8080/api/v1/organizations');
        if (orgsRes.ok()) {
          const orgs = await orgsRes.json();
          if (orgs.content && orgs.content.length > 0) {
            return true;
          }
        }
      } catch (e) {
        // Ignore connection errors and retry
      }
      return false;
    }, { timeout: 30000 }).toBeTruthy();

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

    await expect(page.getByRole('heading', { name: 'Sign In' })).toBeVisible();

    // Fill invalid credentials
    const tenantIdLocator = page.locator('#tenantId');
    await tenantIdLocator.waitFor({ state: 'visible', timeout: 15000 });
    await tenantIdLocator.fill('123e4567-e89b-12d3-a456-426614174000');
    await page.fill('#email', 'nonexistent@example.com');
    await page.fill('#password', 'wrongpassword');
    
    await page.getByRole('button', { name: 'Sign in' }).click();

    // Wait for the error message
    await expect(page.getByText('Invalid credentials or account locked.')).toBeVisible();
  });

  test('should allow user to request a password reset', async ({ page, request }) => {
    // Wait for the backend to be fully seeded with retry logic
    await expect.poll(async () => {
      try {
        const orgsRes = await request.get('http://127.0.0.1:8080/api/v1/organizations');
        if (orgsRes.ok()) {
          const orgs = await orgsRes.json();
          if (orgs.content && orgs.content.length > 0) {
            return true;
          }
        }
      } catch (e) {
        // Ignore connection errors and retry
      }
      return false;
    }, { timeout: 30000 }).toBeTruthy();

    await expect.poll(async () => {
      try {
        const response = await page.goto('http://127.0.0.1:3000/login', { timeout: 5000 });
        return response && response.status() === 200;
      } catch (e) {
        return false;
      }
    }, { timeout: 30000 }).toBeTruthy();
    
    // Click Forgot Password
    await page.getByRole('link', { name: 'Forgot password?' }).click();

    await expect(page.getByRole('heading', { name: 'Forgot password?' })).toBeVisible();

    // Fill reset form
    const resetTenantIdLocator = page.locator('#tenantId');
    await resetTenantIdLocator.waitFor({ state: 'visible', timeout: 15000 });
    await resetTenantIdLocator.fill('123e4567-e89b-12d3-a456-426614174000');
    await page.fill('#email', 'user@example.com');
    
    await page.getByRole('button', { name: 'Send reset link' }).click();

    // Verify generic success message
    await expect(page.getByText('Check your email')).toBeVisible();
    await expect(page.getByText('If an account exists for user@example.com')).toBeVisible();
  });
});
