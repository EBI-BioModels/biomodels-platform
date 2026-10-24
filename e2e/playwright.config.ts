import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end UI tests for BioModels. They run against a server that is already up;
 * start it with `./grailsw run-app` from the repository root.
 *
 *   E2E_BASE_URL  where the app is served (default http://localhost:8080/biomodels)
 *   E2E_MODEL_ID  a published model that exists on that server (default MODEL1801250002)
 */
const baseURL = (process.env.E2E_BASE_URL || 'http://localhost:8080/biomodels').replace(/\/+$/, '') + '/';

export default defineConfig({
  testDir: './tests',
  timeout: 45_000,
  expect: { timeout: 10_000 },
  fullyParallel: true,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } } },
    { name: 'mobile', use: { ...devices['Desktop Chrome'], viewport: { width: 400, height: 800 } } },
  ],
});
