# Risks of removing the EBI Visual Framework from BioModels

Investigation date: 2026-10-04. Based on a grep-level survey of the repo; the app was not run, so nothing here is verified in a browser.

"EBI Visual Framework" here means **EBI-Framework v1.3** (a Foundation 6 build) plus **EBI-Icon-fonts v1.3**, both loaded from `ebi.emblstatic.net`.

## Decision (2026-10-24)

Keep **Foundation 6** as the CSS framework and remove only the EMBL-EBI additions: the injected black bar, global nav and global footer, the EBI scripts, the EBI icon font and the EBI-specific styling. There is no move to the UFL design system or another framework. The sections below were written before this decision; where they talk about replacing the framework, read it as "vendoring plain Foundation and stripping the EBI parts". The Jira epic is JBM-809.

### What the bundle actually contains

- `ebi-global.css` is the full Foundation CSS **6.3.1** with minor EMBL-EBI edits, plus EBI-only rules (`.masthead-black-bar`, `.global-nav`, `.global-footer`, `.masthead`, `#local-title`, `.ebi-color`).
- `foundation.js` in the bundle is Foundation **6.6.3**, so the CSS and JS versions already differ. Vendoring needs one version for both and a page-by-page check.
- `script.js` injects the EBI black bar, global nav and footer, **and the EBI cookie banner** (a data-protection banner with a versioned cookie that links to EMBL-EBI's privacy notice). Decision: the banner stays but is rebuilt locally for the new policy (JBM-814); `jquery.cookiebar.js` and a local Privacy Policy page already exist in the repo.
- The **local sticky masthead** (title, search, nav; Foundation `data-sticky`) stays. Only the EBI black bar and global footer go. Removing the EBI header and footer must leave title, search and nav working: the masthead is anchored to `#content` (`_header.gsp`, `_mainbody.gsp`), its styling (`.masthead`, `#local-title`) currently comes from `ebi-global.css` and has to be ported locally, and the nav relies on Foundation `data-dropdown-menu`.
- The cookie banner depends on a new privacy policy that the BioModels team is preparing (JBM-824, which blocks JBM-814).
- `foundationExtendEBI.js` is EBI-only (Google Analytics load polling and small extensions).

## What the app takes from it

| Dependency | Where | Size |
|---|---|---|
| `ebi-global.css` (Foundation 6 grid, buttons, callouts, reset, typography) | `grails-app/views/templates/biomodels/_head.gsp:73` | Used on nearly every page |
| EBI icon fonts (`icon icon-common icon-*`, `icon-functional`, `icon-fileformats`, `icon-socialmedia`) | Many views; CSS loaded in `_head.gsp:76` | About 25 distinct icon classes |
| `script.js` injects the EBI black bar, global nav and global footer (`#masthead-black-bar`, `#global-nav-expanded`, `#ebi-footer-meta`) | `_loadjs.gsp:3`, `_header.gsp`, `_footer.gsp` | Layout skeleton |
| `foundation.js` + `foundationExtendEBI.js`, started by `$(document).foundation()` | `_loadjs.gsp:7-10` | Powers reveal, accordion, orbit, dropdown-menu, sticky, toggler |
| `elixirBanner.js` + ELIXIR kitemark image | `_footer.gsp:43,73` | Footer ribbon |
| Masthead meta tags and background image | `_head.gsp:40-41` | Cosmetic |

## Usage counts (files)

| Feature | Files |
|---|---|
| `small-N` / `medium-N` / `large-N` | 95 |
| `row` / `columns` | 73 |
| `button` / `callout` / `label` / `badge` | 66 |
| `show-for-*` / `hide-for-*` | 14 |
| `data-accordion` | 14 |
| `data-reveal` | 6 |
| `.foundation(` / `Foundation.` | 6 |
| EBI ids (`#masthead`, `#local-masthead`, `#content`...) | 9 |
| `data-dropdown-menu` / `top-bar` / `title-bar` | 3 |
| `data-sticky` | 2 |
| `data-orbit` | 1 |

The layout is the biggest exposure. The grid and utility classes are not defined anywhere in the repo.

## JBM-810 findings (2026-10-24)

Method: downloaded stock Foundation 6.3.1 (cdnjs) and the EBI v1.3 bundle, compared them by CSS class name (a rule-by-rule diff is too noisy because EBI regroups selectors), and cross-checked the classes against every `*.gsp` and `*.js` file in the repo. Class usage was detected in `class="..."` attributes and `addClass`/`removeClass`/`toggleClass`/`hasClass` calls. Nothing was checked in a browser.

### Recommendation: Foundation 6.6.3 for both CSS and JS, precompiled and vendored

- The JS already running in production is 6.6.3 (the EBI `foundation.js`, with all 20 plugins plus SmoothScroll). Keeping 6.6.3 leaves JS behaviour unchanged. Going to 6.3.1 would downgrade the JS that is live today.
- Only the CSS changes version, from 6.3.1 to 6.6.3. That is the part to check page by page.
- Use the official precompiled `foundation.css` and `foundation.js` committed under `web-app`. A Sass build is not needed: the EBI customisations to keep are small (see below) and go into one local stylesheet.
- Not verified: the visual differences between 6.3.1 and 6.6.3 CSS. That is what the page-by-page check in JBM-811 and JBM-816 covers.

### `ebi-global.css` versus stock Foundation 6.3.1

| | Stock 6.3.1 | `ebi-global.css` |
|---|---|---|
| Size | 102,684 B | 178,662 B |
| Distinct classes | 401 | 693 |
| Only in this file | 5 (`card`, `card-divider`, `card-section`, `menu-bordered`, `menu-hover`) | 297 |

- BioModels uses `card` (2 files) and `card-section` (1 file). Those classes are not styled today, because `ebi-global.css` omits them. Stock Foundation would start styling them, so check those pages.
- EBI base edits that BioModels sees: `body` font (Helvetica, Arial, FreeSans), `select { width: 100% }` (the reason for the workaround in `userAdministration/index.gsp`), `h1` size and margin, and `html`/`body` resets.

EBI-only rules by purpose (from the CSS):

| Purpose | Rules | Declarations | Action |
|---|---|---|---|
| Black bar, global nav, global footer (`.masthead-black-bar`, `.global-nav`, `.global-footer`, `#ebi-footer-meta`) | 65 | about 139 | Drop |
| Local masthead (`.masthead`, `.masthead-inner`, `#local-title`, `#local-search`, `.active-trail`, `.local-footer`) | 38 | about 70 | Port to local CSS |
| Utilities (`size-*`, `padding-*-*`, `margin-*-*`) | 69 | about 69 | Port the ones used |
| Other helpers (`clearable`, `no-underline`, `readmore`, `secondary-background`, `external`, `align-center`, `nowrap`) | 18 | about 41 | Port the ones used |

EBI-only classes actually used in BioModels views (about 40): the structural ones (`masthead-black-bar`, `masthead`, `masthead-inner`, `global-footer`, `local-footer`, `ebi-footer-meta`) belong to JBM-812. The utilities and helpers need porting: `size-75/100/125/150/400`, `padding-top-none` and the other `padding-*`/`margin-*` helpers, `clearable`, `no-underline`, `readmore`, `secondary-background`, `active-trail`, `external`, `align-center`, `nowrap`, `full-width`. Some generic names (`block`, `home`, `search`, `tag`, `clear`, `inline`, `hidden`, `code`) may be BioModels' own classes that happen to match; check before porting.

### JavaScript

- Foundation widgets BioModels uses: Reveal (modals), Accordion (FAQ, dashboards), DropdownMenu (main nav), Orbit (home carousel), Sticky (masthead), Toggler. All are in the 6.6.3 bundle.
- `foundationExtendEBI.js` (EBI-only) provides: external links opening in a new tab (`addBlankTargetToExternalLinkEBI`, `addExternalToBlankWindowLinksEBI`), smooth scrolling for anchor links (`smoothScrollAnchorLinksEBI`), responsive menu setup (`invokeResponsiveMenuEBI`), the EMBL dropdown (`activateEMBLdropdown`) and Google Analytics helpers. Decide per behaviour whether to keep it: external links in a new tab and smooth scroll are visible to users. The views already carry about 255 explicit `target="_blank"` attributes.
- `script.js` provides the black bar, global nav and footer, the data-protection (cookie) banner and remote EBI announcements (`loadRemoteAnnouncements`, `ebiInjectAnnouncements`). The announcements go away with the script.
- The Google Analytics block in `_loadjs.gsp` is commented out, so no analytics is live and there is nothing to preserve.

### Branding variants

- `Config.groovy` sets `jummp.branding.deployment` and `jummp.branding.style` to `biomodels`.
- The `jummp` and `ddmore` layouts and templates have their own head templates and do **not** reference the EBI CDN; only `templates/biomodels/` does. They are not affected by this epic. (The earlier statement that the variants share the EBI classes was wrong.)
- Those two layouts already use `jquery.cookiebar.js`, a working option for the cookie banner in JBM-814.

### Still open for JBM-810

- Brand and logo-usage guidelines from the Earlham Institute and the University of Florida (needs the institutions).
- A per-view inventory of Foundation grid and component classes (aggregate counts are above; a per-view list is only needed when the risky pages are changed).

## Risks, highest first

1. **Layout breaks everywhere (high).** Without the CSS, pages collapse to a single column and responsive hiding stops working. The classes come only from the CDN, so replacing the link means vendoring Foundation 6 or rewriting the markup.

2. **Interactive widgets stop working (high).**
   - Modals (`data-reveal` / `data-open`): search tips, domain-switcher help, feedback form (`rate_review_form`), file preview.
   - Accordions: FAQ, admin and curator dashboards. `faq.gsp:1250,1254` call `.foundation('up'|'down')` directly and will throw.
   - Main nav (`data-dropdown-menu` in `_navbar.gsp`), home-page orbit carousel (`_hp-row-1.gsp`), sticky header (`_header.gsp`, `_mainbody.gsp`).
   - If `foundation.js` is removed but the `$(document).foundation()` call stays, it throws. That may stop later inline scripts on the page.

3. **Global EBI navigation and footer vanish (medium).** `#masthead-black-bar` and `#global-footer` are empty shells filled in by `script.js`. Removing it leaves empty containers and drops the EMBL-EBI links and footer legal text.

4. **Compliance and branding content disappears (medium).**
   - The ELIXIR Deposition Database banner goes with `elixirBanner.js`; decide whether to keep it.
   - `_embl-ebi-data-preservation-statement.gsp` edits the `.elixir-ribbon` DOM that `elixirBanner.js` creates, so it silently does nothing without it.
   - Footer and statement text will need rewriting for the University of Florida / LSM move.

5. **Existing CSS overrides are tied to the framework (medium).**
   - `web-app/css/common.css`, `web-app/css/ebilayout.css` and inline `<style>` blocks assume `ebi-global` defaults.
   - Two views document workarounds against it: `jummp-plugin-web-application/.../templates/model/submit/_uploadingFiles.gsp:7` and `.../userAdministration/index.gsp:91` (`select { width: 100% }`).
   - Expect small regressions in spacing, link underlines and form controls.

6. **Icons go blank (medium).** Without the icon font, icons render as empty boxes or nothing. The file-format and social icons (`icon-fileformats`: XML, RDF_XML, JSON; `icon-socialmedia`) are not in Font Awesome 4.7, which is already vendored in `web-app/css/font-awesome-4.7.0`. Each needs a mapping or a replacement.

7. **Other branding variants (low, resolved by JBM-810).** The `jummp` and `ddmore` layouts have their own head templates and do not load the EBI CDN, so they are not affected. Only `templates/biomodels/` is. The plugin views that use Foundation classes render under the `biomodels` style (the configured default).

8. **Plugin views are not isolated (medium).** Plugin GSPs, mostly `jummp-plugin-web-application` and `jummp-plugin-biomodels-dom` (`parameterSearch/index.gsp:28` prefetches the CDN), use the same classes. The change has to cover the plugins too, and plugin unit specs do not run from the root.

9. **Testing gaps (medium).** No visual or browser tests were found; Selenium or functional tests were not searched for, so check that first. Grails unit and integration tests will not catch any of this, so regressions will only show up in manual or browser testing.

10. **Operational side effects (low).**
    - Plus: no third-party script on the login and 2FA pages, and one less point of failure.
    - jQuery 1.11.1 and jQuery UI come from `code.jquery.com`, separately from the framework. The help panel in `main.gsp` needs them, so do not remove them by accident.
    - Markup changes can affect how search engines index the pages; crawl a few key pages afterwards.

## Suggested approach

1. **Decide the target first.** Either keep Foundation 6 but vendor it locally, or move to another CSS framework or custom CSS. The choice sets the scope; a rewrite touches roughly 150 GSPs.
2. **Do it in layers, one PR each, with small one-concern commits:**
   - Vendor `foundation.js` and `ebi-global.css` locally, with no behaviour change.
   - Replace the global header and footer with local markup.
   - Replace the icons.
   - Replace the widgets one by one: modals, accordion, nav, orbit.
   - Remove the CDN links.
3. **Keep a checklist of the high-risk pages:** home, search results, model display, submit wizard, login with 2FA, FAQ, admin and curator dashboards, user administration.
4. **Verify in real browsers** at desktop and mobile widths. Watch the console for `foundation is not a function` errors.

## Method and caveats

- Greps over `*.gsp`, `*.js` and `*.groovy`, excluding minified and jQuery files, plus reading `_head.gsp`, `_loadjs.gsp`, `_header.gsp`, `_footer.gsp` and `layouts/biomodels/main.gsp`.
- Counts are file counts and may include false positives, for example `small-N` matching unrelated class names.
- An attempted per-plugin breakdown was invalid (the pathspec matched the whole repo) and is not reported.
- Not checked: Selenium or functional tests, which branding variants are deployed, and the actual rendered output.
