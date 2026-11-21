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

/** Wait until the EBI script.js has filled the bar and footer containers that the tests then expect to be hidden. */
async function waitForEbiScript(page: Page) {
  await page.waitForFunction(() => ['#masthead-black-bar', '#global-nav-expanded', '#ebi-footer-meta']
    .every((selector) => (document.querySelector(selector)?.childElementCount ?? 0) > 0));
}

test('home page panels sit side by side on desktop and stack on mobile', async ({ page }) => {
  await page.goto('');
  await waitForFoundation(page);
  // The panels are found by their headings, which are content: skip rather than fail when an editor renames one.
  for (const title of [/Model of The Month/i, /Browse by Modelling Approach/i, /^\s*News\s*$/]) {
    test.skip((await page.locator('h3', { hasText: title }).count()) === 0, `no home page panel titled ${title}`);
  }
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

test('the ELIXIR banner is rendered locally, with its kitemark and without EMBL-EBI links', async ({ page }) => {
  const ebiRequests: string[] = [];
  page.on('request', (r) => { if (/elixirBanner\.js/.test(r.url())) ebiRequests.push(r.url()); });
  await page.goto('');
  await waitForFoundation(page);
  const ribbon = page.locator('#elixir-banner .elixir-ribbon');
  await expect(ribbon).toBeVisible();
  await expect(ribbon).toContainText('BioModels is an ELIXIR Deposition Database');
  await expect(ribbon.locator('a[href*="elixir-deposition-databases"]')).toHaveCount(1);
  // The EMBL-EBI licensing and data preservation links that used to be added to the ribbon are gone.
  await expect(ribbon.getByRole('link', { name: /EMBL-EBI/ })).toHaveCount(0);
  await expect(ribbon.locator('a[href*="ebi.ac.uk"]')).toHaveCount(0);
  const kite = await ribbon.locator('.elixir-logo-kite').evaluate((e) => getComputedStyle(e).backgroundImage);
  expect(kite).toContain('/images/biomodels/elixir_kitemark-60px.png');
  expect(ebiRequests).toEqual([]);
});

test('the EMBL-EBI black bar and global footer are not shown, the local masthead is', async ({ page }) => {
  await page.goto('');
  await waitForFoundation(page);
  // The EBI script.js still fills these (for its cookie banner): check only after it has, or "hidden" proves nothing.
  await waitForEbiScript(page);
  await expect(page.locator('#masthead-black-bar')).toBeHidden();
  await expect(page.locator('#global-footer')).toBeHidden();
  await expect(page.locator('#global-nav')).toBeHidden();
  await expect(page.getByRole('link', { name: /^About us$/ }).filter({ visible: true })).toHaveCount(0);
  await expect(page.locator('#masthead #localsearch')).toBeVisible();
});

test('the footer lists BioModels pages in columns and links nowhere to EMBL-EBI', async ({ page }) => {
  await page.goto('');
  await waitForFoundation(page);
  await waitForEbiScript(page);
  const footer = page.locator('#biomodels-footer');
  await expect(footer).toBeVisible();
  for (const heading of ['Browse', 'Submit and curate', 'Support', 'About']) {
    await expect(footer.getByRole('heading', { name: heading })).toBeVisible();
  }
  for (const link of ['FAQs', 'Privacy Policy', 'Terms of Use', 'Contact us']) {
    await expect(footer.getByRole('link', { name: link })).toBeVisible();
  }
  await expect(footer.locator('a[href*="ebi.ac.uk"]')).toHaveCount(0);
  await expect(footer).toContainText(/Earlham Institute.*Laboratory for\s+Systems Medicine/s);
  // Visited and unvisited links look the same, and the footer text is smaller than the main text.
  const sizes = await page.evaluate(() => {
    const px = (s: string) => parseFloat(getComputedStyle(document.querySelector(s)!).fontSize);
    return { body: px('body'), heading: px('#biomodels-footer h5'), link: px('#biomodels-footer li a'),
      meta: px('.biomodels-footer-meta') };
  });
  expect(await footer.locator('h5').first().evaluate((e) => getComputedStyle(e).textTransform)).toBe('uppercase');
  expect(sizes.heading).toBeLessThan(sizes.body);
  expect(sizes.link).toBeLessThan(sizes.body);
  expect(sizes.meta).toBeLessThan(sizes.body);
  // The four columns sit side by side on desktop and two by two on mobile.
  const tops = await footer.locator('h5').evaluateAll((els) => els.map((e) => Math.round(e.getBoundingClientRect().top)));
  expect(new Set(tops).size).toBe(isMobile() ? 2 : 1);
});

test('the EBI cookie banner still shows, fixed at the bottom of the window', async ({ page }) => {
  await page.goto('');
  await waitForFoundation(page);
  // script.js adds the banner and styles it inline, so it must not depend on the deleted ebi-bar-temporary.css.
  const banner = page.locator('#data-protection-banner');
  await expect(banner).toBeVisible();
  expect(await banner.evaluate((e) => getComputedStyle(e).position)).toBe('fixed');
  const box = await banner.boundingBox();
  const viewport = page.viewportSize()!;
  expect(box!.y + box!.height).toBeGreaterThan(viewport.height - 2);
});
