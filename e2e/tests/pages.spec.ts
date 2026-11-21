
import { expect, test, MODEL_ID, skipUnlessModelExists, waitForFoundation, watchProblems } from './support';

const PAGES: Array<[string, string, RegExp]> = [
  ['home', '', /BioModels/],
  ['search results', 'search?query=glycolysis', /Model Search/],
  ['FAQ', 'faq', /Frequently Asked Questions/],
  ['login', 'login/auth', /Login/],
  ['acknowledgements', 'acknowledgements', /Acknowledgements/],
  ['model', MODEL_ID, /\| BioModels/],
];

for (const [name, path, title] of PAGES) {
  test(`${name} page loads with Foundation and without errors`, async ({ page }) => {
    if (path === MODEL_ID) await skipUnlessModelExists(page);
    const problems = watchProblems(page);
    const response = await page.goto(path);
    expect(response?.status()).toBe(200);
    await expect(page).toHaveTitle(title);
    await waitForFoundation(page);
    const major = await page.evaluate(() => Number(String((window as any).Foundation.version).split('.')[0]));
    expect(major).toBe(6);
    // The masthead with the search box is part of every page.
    await expect(page.locator('#local-search-box')).toBeVisible();
    problems.assertClean();
  });
}

test('the acknowledgements card and page name Earlham and the Laboratory for Systems Medicine, with the Earlham logo first', async ({ page }) => {
  const wording = /funded and hosted by the\s+Earlham Institute.*Laboratory for\s+Systems Medicine/s;
  for (const [path, area] of [['', '#acknowledgements'], ['acknowledgements', 'body']] as const) {
    await page.goto(path);
    await waitForFoundation(page);
    const scope = page.locator(area).first();
    await expect(scope).toContainText(wording);
    const logo = scope.locator('img[alt="Earlham Institute logo"]');
    await expect(logo).toBeVisible();
    // The Earlham logo comes before the earlier funders' logos and links to Earlham.
    const order = await scope.locator('img[alt="Earlham Institute logo"], img[alt="EMBL logo"]')
      .evaluateAll((els) => els.map((e) => e.getAttribute('alt')));
    expect(order).toEqual(['Earlham Institute logo', 'EMBL logo']);
    await expect(logo.locator('xpath=ancestor::a[1]')).toHaveAttribute('href', 'https://www.earlham.ac.uk/');
    expect(await logo.evaluate((e: HTMLImageElement) => e.naturalWidth)).toBeGreaterThan(0);
  }
});
