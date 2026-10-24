import { Page } from '@playwright/test';
import { expect, test, isMobile, waitForFoundation } from './support';

/** The bounding box of the nearest grid column around the panel title that matches. */
async function columnOf(page: Page, title: RegExp) {
  const heading = page.locator('h3', { hasText: title }).first();
  await expect(heading).toBeVisible();
  const column = heading.locator('xpath=ancestor::*[contains(concat(" ", normalize-space(@class), " "), " columns ")][1]');
  const box = await column.boundingBox();
  if (!box) throw new Error(`no grid column around ${title}`);
  return box;
}

test('home page panels sit side by side on desktop and stack on mobile', async ({ page }) => {
  await page.goto('');
  await waitForFoundation(page);
  const month = await columnOf(page, /Model of The Month/i);
  const browse = await columnOf(page, /Browse by Modelling Approach/i);
  const news = await columnOf(page, /^\s*News\s*$/);

  if (isMobile()) {
    expect(Math.abs(browse.x - month.x)).toBeLessThan(3);
    expect(Math.abs(news.x - month.x)).toBeLessThan(3);
    expect(browse.y).toBeGreaterThan(month.y + month.height - 5);
    expect(news.y).toBeGreaterThan(browse.y + browse.height - 5);
  } else {
    expect(Math.abs(browse.y - month.y)).toBeLessThan(8);
    expect(Math.abs(news.y - month.y)).toBeLessThan(8);
    expect(browse.x).toBeGreaterThan(month.x + month.width - 5);
    expect(news.x).toBeGreaterThan(browse.x + browse.width - 5);
  }
});

test('the page does not scroll sideways', async ({ page }) => {
  for (const path of ['', 'faq', 'login/auth']) {
    await page.goto(path);
    await waitForFoundation(page);
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
    expect(overflow, `horizontal overflow on /${path}`).toBeLessThanOrEqual(1);
  }
});

test('Foundation grid and responsive utilities apply', async ({ page }) => {
  await page.goto('');
  await waitForFoundation(page);
  // .hide-for-small-only hides below 40em and shows above it.
  const hidden = await page.evaluate(() => {
    const probe = document.createElement('div');
    probe.className = 'hide-for-small-only';
    probe.textContent = 'probe';
    document.body.appendChild(probe);
    const display = getComputedStyle(probe).display;
    probe.remove();
    return display === 'none';
  });
  expect(hidden).toBe(isMobile());
});
