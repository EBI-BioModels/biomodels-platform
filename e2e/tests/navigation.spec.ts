
import { expect, test, isMobile, waitForFoundation, watchProblems } from './support';

test('searching from the masthead shows results', async ({ page }) => {
  const problems = watchProblems(page);
  await page.goto('');
  await waitForFoundation(page);
  await page.locator('#local-search-box').fill('glycolysis');
  await page.locator('#btn-search-submit').click();
  await expect(page).toHaveURL(/search\?.*query=glycolysis/);
  await expect(page).toHaveTitle(/Model Search/);
  await expect(page.locator('body')).toContainText(/glycolysis/i);
  problems.assertClean();
});

test('the masthead title links back to the home page', async ({ page }) => {
  test.skip(isMobile(), 'the title is hidden on small screens');
  await page.goto('faq');
  await waitForFoundation(page);
  await page.locator('#local-title a').first().click();
  await expect(page).toHaveURL(/\/biomodels\/?$/);
});

test('desktop: the Browse menu opens on hover and closes again', async ({ page }) => {
  test.skip(isMobile(), 'desktop menu');
  await page.goto('');
  await waitForFoundation(page);
  const item = page.locator('#local-nav a', { hasText: /All\s+models/ });
  await expect(item).toBeHidden();
  await page.locator('#local-nav > li > a', { hasText: /^\s*Browse\s*$/ }).hover();
  await expect(item).toBeVisible();
  await page.mouse.move(700, 600);
  await expect(item).toBeHidden();
});

test('mobile: the hamburger button reveals the menu', async ({ page }) => {
  test.skip(!isMobile(), 'mobile menu');
  await page.goto('');
  await waitForFoundation(page);
  const home = page.locator('#local-nav a', { hasText: /^\s*Home\s*$/ });
  await expect(page.locator('#biomodels-hamburger')).toBeVisible();
  await expect(home).toBeHidden();
  await page.locator('#biomodels-hamburger').click();
  await expect(home).toBeVisible();
  await page.locator('#biomodels-hamburger').click();
  await expect(home).toBeHidden();
});

test('the masthead stays usable after scrolling', async ({ page }) => {
  await page.goto('');
  await waitForFoundation(page);
  await page.mouse.wheel(0, 900);
  await page.locator('#local-search-box').scrollIntoViewIfNeeded();
  await expect(page.locator('#local-search-box')).toBeVisible();
  await page.locator('#local-search-box').fill('mapk');
  await expect(page.locator('#local-search-box')).toHaveValue('mapk');
});
