# BioModels end-to-end tests

Browser tests, written with [Playwright](https://playwright.dev), that check the public pages of a running
BioModels server: layout, Foundation widgets (modals, accordions, carousel, menus), the masthead search and the
model page. They guard the front end while the EMBL-EBI Visual Framework is removed (JBM-809) and do not depend on
anything EMBL-EBI specific.

## Run

```bash
./grailsw run-app            # from the repository root, in another terminal
cd e2e
npm install
npx playwright install chromium   # first time only
npm test
```

| Variable | Default | Meaning |
|---|---|---|
| `E2E_BASE_URL` | `http://localhost:8080/biomodels` | where the app is served |
| `E2E_MODEL_ID` | `MODEL1801250002` | a published model that exists on that server |

The suite runs twice, as the `desktop` (1440 px) and `mobile` (400 px) projects. `npm run report` opens the HTML report.
Failed tests keep a screenshot and a trace in `test-results/`.

## Finding out what depends on a resource

`E2E_BLOCK` is a regular expression; requests whose URL matches it are aborted during the tests.

```bash
E2E_BLOCK='foundationExtendEBI' npm test      # one script
E2E_BLOCK='ebi\.emblstatic\.net' npm test     # the whole EMBL-EBI CDN
```

Result when this was last tried (JBM-811): in both cases the same 22 tests fail, all at the final "no script errors"
check, because the page's own inline calls (for example `$(document).foundationExtendEBI()` in `_loadjs.gsp`) throw once
the script is missing. Every functional check passes: the grid, modals, accordions, menus (including the mobile
hamburger), the carousel, search and the model tabs. Remove the inline calls together with the scripts (JBM-812,
JBM-814) and the suite should then pass with the CDN blocked.

Not covered, so check by hand when removing the matching script: external links opening in a new tab, smooth
scrolling for anchor links, icons, the EBI header and footer, and the cookie banner.

## Scope and limits

- Public pages only. Login needs an email one-time code (2FA), so the submission wizard, the dashboards and user
  administration are not covered.
- Needs a server with data: the model page test uses `E2E_MODEL_ID`, and the search test needs at least one
  model that matches "glycolysis".
- Failures of third-party hosts (the embedded Twitter timeline, CDNs) are ignored; failures of the app's own assets
  and uncaught script errors fail the test.
