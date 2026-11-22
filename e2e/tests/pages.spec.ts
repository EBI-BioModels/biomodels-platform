
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
    await expect(scope).toContainText(/Previously, BioModels was hosted by EMBL-EBI/);
    const logo = scope.locator('img[alt="Earlham Institute logo"]');
    await expect(logo).toBeVisible();
    // The Earlham logo comes before the earlier funders' logos and links to Earlham.
    const order = await scope.locator('img[alt="Earlham Institute logo"], img[alt="EMBL logo"]')
      .evaluateAll((els) => els.map((e) => e.getAttribute('alt')));
    expect(order).toEqual(['Earlham Institute logo', 'EMBL logo']);
    await expect(logo.locator('xpath=ancestor::a[1]')).toHaveAttribute('href', 'https://www.earlham.ac.uk/');
    // The block-level logo must stay inside its column, also on a narrow screen.
    const box = await logo.boundingBox();
    expect(box!.x + box!.width).toBeLessThanOrEqual(page.viewportSize()!.width);
    // The image may still be loading when it becomes visible, so poll instead of reading naturalWidth once.
    await expect.poll(() => logo.evaluate((e: HTMLImageElement) => e.naturalWidth)).toBeGreaterThan(0);
  }
});

test('the acknowledgements page says the BioModels teams develop it and credits the EBI-era teams as previous developers', async ({ page }) => {
  await page.goto('acknowledgements');
  await waitForFoundation(page);
  const body = page.locator('body');
  await expect(body).toContainText('BioModels is now developed and maintained by the BioModels developers and curators teams.');
  // The EBI-era teams stay, but as the previous developers.
  await expect(body).toContainText(/Previously, BioModels was developed by the\s+Molecular Networks team.*SBML Team/s);
  await expect(page.locator('img[alt="EMBL-EBI logo"], img[alt="SBML logo"]')).toHaveCount(2);
});
