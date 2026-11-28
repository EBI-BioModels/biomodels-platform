import { expect, test, waitForFoundation } from './support';

// The pages that show logos and screenshots from the static assets server.
const PAGES = ['', 'acknowledgements', 'curation-docs/annotation', 'curation-docs/annotation-tips'];

for (const path of PAGES) {
  test(`/${path} asks wwwdev.ebi.ac.uk for nothing`, async ({ page }) => {
    const wwwdev: string[] = [];
    page.on('request', (request) => {
      if (new URL(request.url()).hostname === 'wwwdev.ebi.ac.uk') wwwdev.push(request.url());
    });
    const response = await page.goto(path);
    expect(response?.status()).toBe(200);
    await waitForFoundation(page);
    await page.waitForLoadState('load');
    expect(wwwdev).toEqual([]);
  });
}
