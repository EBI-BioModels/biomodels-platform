# Risks of removing the EBI Visual Framework from BioModels

Investigation date: 2026-10-04. Based on a grep-level survey of the repo; the app was not run, so nothing here is verified in a browser.

"EBI Visual Framework" here means **EBI-Framework v1.3** (a Foundation 6 build) plus **EBI-Icon-fonts v1.3**, both loaded from `ebi.emblstatic.net`.

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

7. **Other branding variants share the same assets (medium).** `grails-app/views/layouts/` and `templates/` also have `jummp` and `ddmore` variants. Confirm which are still deployed. The `jummp` and `ddmore` layouts load `jquery.cookiebar` and reuse the EBI classes.

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
