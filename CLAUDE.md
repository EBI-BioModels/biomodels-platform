# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BioModels is the world's largest repository of mathematical models of biological and biomedical systems (2,700+ published models). Built on the [Jummp](https://bitbucket.org/jummp/jummp/wiki/) infrastructure, it provides versioned model storage, multi-format submission (SBML, COMBINE Archive, PharmML, MDL), collaborative curation, and faceted search powered by Solr.

**Production site:** https://www.biomodels.org

**Tech stack:** Grails 2.5.5 (Groovy/Java), GORM/Hibernate, PostgreSQL/MySQL, Apache Solr 5.4.1, Spring Security, Quartz, Git-backed VCS, Tomcat 7 (production), Docker.

## Institutional Context

BioModels was developed and hosted at EMBL-EBI (European Bioinformatics Institute) for ~20 years with European Commission funding. In 2025, it migrated to the **Laboratory for Systems Medicine (LSM)**, Department of Medicine, Division of Pulmonary – Systems Medicine, University of Florida.

- LSM BioModels page: https://systemsmedicine.pulmonary.medicine.ufl.edu/biomodels/
- Contact: DOM-LabforSysMedicin@ad.ufl.edu
- EBI migration announcement: https://www.ebi.ac.uk/about/news/updates-from-data-resources/biomodels-moves-university-of-florida/

## Security Enhancement Initiative (2026)

A mandatory security rollout went live **04 May 2026**, announced to all users by email. Features added:

| Feature                         | Detail                                                           |
|---------------------------------|------------------------------------------------------------------|
| Two-Factor Authentication (2FA) | Email-based OTP at login; optional 30-day device trust (JBM-689) |
| Smart Account Lockout           | 1-hour lock after 3 consecutive failed login attempts (JBM-686)  |
| Compromised Password Monitoring | Cross-checks passwords against known breach databases            |
| Updated Password Standards      | Stricter complexity and history rules                            |

Email templates for this campaign are in `logs/context/` (`email_template.txt`, `email_template.html`). The HTML template uses UF/LSM brand colours: orange `#ED6B21` (accent) and deep navy `#072C55` (header/headings). Use these colours for any future user-facing HTML emails.

## Email Conventions

### Subject lines
All transactional emails must follow the pattern:

```
[BioModels] <action description> to <modelId>
```

Examples:
- `[BioModels] Tung Nguyen has added you as a contributor to MODEL2401190005`
- `[BioModels] Tung Nguyen has invited you to contribute to MODEL2401190005 as a Curator`
- `[BioModels] Your contributor access to MODEL2401190005 has been removed`
- `[BioModels] Your 2FA has been Enabled`
- `[BioModels] Your Verification Code`

Rules:
- Always prefix with `[BioModels]` — helps users filter emails and identifies the sender at a glance
- Include the model ID in the subject when the email is about a specific model
- Drop trailing "on BioModels" — the prefix already identifies the product
- Keep subjects concise; put details in the email body

### HTML template
Use the standard two-panel HTML layout (orange accent bar + navy header + white body + grey footer). Sender address is `noreply@biomodels.org`. Contact address is `contact@biomodels.org`.

## Commands

```bash
# Development server (port 8080)
./grailsw run-app

# Run all tests
./grailsw test-app

# Run a specific test class
./grailsw test-app net.biomodels.jummp.core.AuthServiceSpec

# Run tests with coverage
./grailsw test-app -coverage && ./grailsw coverage-report

# Build WAR for deployment
./grailsw war

# Clean build artifacts
./grailsw clean

# Run tests for a specific plugin (each plugin has its own `grailsw`)
cd jummp-plugins/jummp-plugin-<name> && ./grailsw test-app <FullyQualifiedTestClassName>
```

Test reports go to `target/test-reports/`. Coverage reports use Cobertura.

Dependencies are resolved via Maven (configured in `grails-app/conf/BuildConfig.groovy`). If a private Artifactory instance is available, set `JUMMP_ARTIFACTORY_URL` in the environment.

## Architecture

### Plugin-based modular structure

The application is split into a core Grails app (`grails-app/`) and 23+ plugins under `jummp-plugins/`. Each plugin is a self-contained Grails plugin with its own controllers, services, domain classes, and tests.

Key plugins:
- `jummp-plugin-core-api` — shared interfaces, DTOs, and domain adapters used across plugins
- `jummp-plugin-git` — Git-backed versioned file storage for models (via JGit)
- `jummp-plugin-sbml` — SBML format parsing and validation (via JSBML)
- `jummp-plugin-combine-archive` — COMBINE Archive format support
- `jummp-plugin-configuration` — externalised application configuration
- `jummp-plugin-jms` — JMS messaging (background job coordination; disabled by default, only active when `JUMMP_EXPORT=jms`)
- `jummp-plugin-annotation-*` — biological entity annotation
- `jummp-plugin-web-application` — main MVC web layer; also hosts `Team`/contributor management (the former `jummp-plugin-security` plugin was deleted in 2019 and merged in here)

Note: there is no `jummp-plugin-security` or `jummp-plugin-solr` plugin. Spring Security/2FA configuration lives directly in `grails-app/conf/Config.groovy` plus `grails-app/controllers/.../security/AuthController.groovy` and `grails-app/conf/VerifyOtpFilters.groovy`. Solr integration lives in `src/groovy/net/biomodels/jummp/search/` (`SolrBasedSearch`, `SolrServerHolder`), not a separate plugin. See `documentation/architecture-analysis.md` for the full plugin inventory and dependency graph (the checked-in `documentation/plugin-dependency.dot` is stale and still shows the deleted security plugin).

### Request flow

URL routing is defined in `grails-app/conf/UrlMappings.groovy`. Incoming requests go to controllers in `grails-app/controllers/net/biomodels/jummp/`, which delegate to services in `grails-app/services/net/biomodels/jummp/core/`. Key services:

- `ModelService` — model lifecycle (create, update, publish, delete)
- `SubmissionService` — multistep model submission workflow
- `SearchService` — Solr-backed search and faceting
- `VcsService` — version control operations (delegates to git plugin)
- `FileSystemService` — file storage and retrieval

### Domain model

`grails-app/domain/net/biomodels/jummp/` only contains a handful of local domain classes: `Feedback`, `model.ModelAudit`, `model.ModelHistoryItem`, `security.TwoFactorAuth`. `Team`/`UserTeam` live under `jummp-plugin-web-application`.

The central entities — `Model`, `Revision`, `Publication`, `PublicationLinkProvider`, `RepositoryFile`, `ModelFormat`, `Person`, `User`, `Role`, `UserRole` — are **not present as source in this repository**. They were moved out to a separate Maven project in 2015 (commit `3e92bbf70`) and, for the security-related classes, formerly lived in a `jummp-plugin-security` plugin that was deleted in 2019 (commit `2e9bf35e5`) once its remaining wiring was merged into `jummp-plugin-web-application`. That separate project is **`AnnotationStore`**, declared as `compile "net.biomodels.jummp:AnnotationStore:0.3.6"` in `grails-app/conf/BuildConfig.groovy` — checked out locally as a sibling repo at `../annotationstore` (relative to this repo). Its domain classes live under `src/main/groovy/net/biomodels/jummp/{model,plugins.security}/`. To read or edit these classes' fields/constraints, open that sibling checkout directly; schema changes require bumping the `AnnotationStore` version in `BuildConfig.groovy` (and the corresponding version references in `jummp-plugin-configuration`/`jummp-plugin-core-api`) and cutting a new release there. Code in this repo consumes them via wrapper/adapter classes in `jummp-plugin-core-api/src/groovy/.../core/adapters/` (`ModelAdapter`, `RevisionAdapter`, `PublicationAdapter`, `PersonAdapter`, etc.), which convert them to `*TransportCommand` DTOs. See `documentation/architecture-analysis.md` §3 for full field-level detail on each class.

### Revision deletion (JBM-349)

Deleting a `Revision` (`ModelService.deleteRevision`) is a soft delete at the DB layer but a
**hard, one-way delete at the VCS layer** — this asymmetry is intentional, not a bug:

- **DB**: just flips `revision.deleted = true` and saves; the row stays in `revision` forever
  (audit trail, `revisionNumber` gap preserved, still queryable). There is no
  `undeleteRevision` anywhere (unlike `Model`, which has `undeleteModel`) — nothing ever
  flips the flag back.
- **VCS**: the underlying git commit is actually excised from the branch's reachable
  history via `VcsService`/`GitManager` (`jummp-plugin-git`), not just soft-flagged:
  - Mid-history revision: `GitManager.deleteCommit` cherry-pick-replays every later commit
    onto the deleted one's parent and force-moves the branch; the original commit becomes
    unreachable. If a replayed commit's own diff is empty (e.g. a metadata-only
    resubmission), it forces a real (empty) commit anyway — two revisions can never share
    one commit sha, since `Revision.vcsId` is `unique:'model'`.
  - Latest revision: `VcsService.resetModelRepository` hard-resets the branch pointer to
    the previous revision's commit instead — no replay needed since nothing comes after it.
  - An excised commit isn't instantly gone (it lingers as a loose object until git's next
    GC prunes it), but there is no porcelain path back to it, and later revisions' `vcsId`s
    get remapped away from it — so it is never referenced again.

The model's **first revision (`revisionNumber == 1`) can never be deleted or flagged minor**,
enforced in `ModelService.deleteRevision`/`setMinorRevision` themselves (not just hidden in
the UI) — it must stay retained and unmodifiable as the model's traceable starting point.

The update-model wizard's "amend" checkbox (`SubmissionService`, `isAmend`) has the same
first-revision protection: it must check the revision actually being amended (the current,
non-deleted latest one)'s own `revisionNumber`, never `Model.revisions.size()` — that raw
collection counts deleted revisions too, which previously let the checkbox appear for a
model reduced to just its first revision by an earlier deletion.

### GSP Layouts

Two site-wide layouts live in `grails-app/views/layouts/biomodels/`:

- **`main.gsp`** — the primary layout used by almost every page (login, registration, user management, auth/2FA views, homepage, maintenance). Pages opt in via `<meta name="layout" content="${session['branding.style']}/main"/>` (or hardcoded `biomodels/main`). Contains the 2FA/enrollment amber banner, contextual help panel, and all shared JS/CSS.
- **`newmain.gsp`** — a slimmer layout used only by `jummp-plugin-web-application/.../views/model/display.gsp`. Delegates contextual-help JS to a separate `setup-contextual-help` template; has no 2FA banner. Treat it as legacy — do not add new pages to it.

When adding a new page, use `main.gsp`. When touching the 2FA banner or global JS/CSS, edit `main.gsp` only.

`display.gsp` itself is a debug/staging alternative to `show.gsp` for the model detail page. `ModelController.show` only renders it when the Redis `DEBUGGING_MODE` flag is `true`; in normal production flow it is never reached.

### Configuration

- `grails-app/conf/Config.groovy` — main app config (Spring Security, CORS, logging, mail)
- `grails-app/conf/DataSource.groovy` — database connections (env-aware)
- `grails-app/conf/BuildConfig.groovy` — dependencies and build settings
- `grails-app/conf/BootStrap.groovy` — startup initialisation

### Testing

Tests use the Spock framework (`.groovy` files in `test/unit/` and `test/integration/`). Unit tests use Grails unit testing mixins; integration tests require a running Spring context. Test fixtures are in `test/files/`.

### Commit style

Commits follow the pattern `JBM-### type: message` where the type is `fix`, `feat`, `chore`, or `docs`, e.g. `JBM-686 fix: skip attempt counting for non-existing users`.
