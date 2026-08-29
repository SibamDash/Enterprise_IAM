import { test, expect } from '@playwright/test';

test.describe('Phase 2: Authentication', () => {
  test('should allow user to navigate to login and see errors for invalid credentials', async ({ page }) => {
    // Navigate to Login directly
    await page.goto('http://localhost:3000/login');
    await expect(page).toHaveTitle(/Enterprise IAM/);

    await expect(page.getByRole('heading', { name: 'Sign In' })).toBeVisible();

    // Fill invalid credentials
    await page.getByLabel('Organization ID (Tenant)').fill('123e4567-e89b-12d3-a456-426614174000');
    await page.getByLabel('Email address').fill('nonexistent@example.com');
    await page.getByLabel('Password').fill('wrongpassword');
    
    await page.getByRole('button', { name: 'Sign in' }).click();

    // Wait for the error message
    await expect(page.getByText('Invalid credentials or account locked.')).toBeVisible();
  });

  test('should allow user to request a password reset', async ({ page }) => {
    await page.goto('http://localhost:3000/login');
    
    // Click Forgot Password
    await page.getByRole('link', { name: 'Forgot password?' }).click();

    await expect(page.getByRole('heading', { name: 'Forgot password?' })).toBeVisible();

    // Fill reset form
    await page.getByLabel('Organization ID (Tenant)').fill('123e4567-e89b-12d3-a456-426614174000');
    await page.getByLabel('Email address').fill('user@example.com');
    
    await page.getByRole('button', { name: 'Send reset link' }).click();

    // Verify generic success message
    await expect(page.getByText('Check your email')).toBeVisible();
    await expect(page.getByText('If an account exists for user@example.com')).toBeVisible();
  });
});
