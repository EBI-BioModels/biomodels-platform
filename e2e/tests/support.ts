import { expect, Page, test as base } from '@playwright/test';

export const MODEL_ID = process.env.E2E_MODEL_ID || 'MODEL1801250002';

/**
 * Third-party hosts whose failed requests are not ours (embedded timelines, CDNs, analytics). Script errors are not
 * filtered by it: the EBI scripts are still ours to remove, so an error inside one of them must show up.
 */
const THIRD_PARTY = /twitter\.com|twimg\.com|d3js\.org|googletagmanager|google-analytics|jsdelivr|code\.jquery\.com|ebi\.emblstatic\.net/;

/**
 * `test` with one extra: when E2E_BLOCK is set to a regular expression, every request whose URL matches it is
 * aborted. Use it to see what depends on a resource, for example
 *   E2E_BLOCK='ebi\.emblstatic\.net' npm test        (the whole EMBL-EBI CDN)
 *   E2E_BLOCK='foundationExtendEBI' npm test            (a single script)
 */
export const test = base.extend({
  page: async ({ page }, use) => {
    if (process.env.E2E_BLOCK) await page.route(new RegExp(process.env.E2E_BLOCK), (route) => route.abort());
    await use(page);
  },
});
export { expect };

export const isMobile = () => base.info().project.name === 'mobile';

/**
 * Records uncaught script errors and failed requests to the app's own origin while a test runs.
 * Call assertClean() at the end of the test.
 */
export function watchProblems(page: Page) {
  const problems: string[] = [];
  const origin = new URL(base.info().project.use.baseURL!).origin;
  page.on('pageerror', (error) => problems.push(`script error: ${error.message}`));
  page.on('requestfailed', (request) => {
    if (request.url().startsWith(origin) && !THIRD_PARTY.test(request.url())) {
      problems.push(`request failed: ${request.url()} (${request.failure()?.errorText})`);
    }
  });
  page.on('response', (response) => {
    if (response.url().startsWith(origin) && response.status() >= 400 && !/favicon/.test(response.url())) {
      problems.push(`HTTP ${response.status()}: ${response.url()}`);
    }
  });
  return {
    assertClean() {
      expect(problems, 'errors raised by the page or its own assets').toEqual([]);
    },
  };
}

/** Waits until Foundation's plugins have been initialised on the page. */
export async function waitForFoundation(page: Page) {
  await page.waitForFunction(() => typeof (window as any).Foundation !== 'undefined', undefined, { timeout: 10_000 });
  await page.waitForLoadState('load');
}
