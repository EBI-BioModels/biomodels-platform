
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
