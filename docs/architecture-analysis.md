# BioModels/Jummp — Architecture Analysis

_Generated 2026-07-04 by reading the repository directly (source tree, git history, build config). Supersedes/corrects some claims in the root `CLAUDE.md` where noted._

## 1. High-level shape

BioModels is a Grails 2.5.5 (Groovy/Java 8) monolith built as a **core app + plugin federation**: a thin `grails-app/` shell (routing, jobs, migrations, a handful of controllers/services/domain classes) that composes ~21 active Grails plugins under `jummp-plugins/`, each a self-contained vertical (own controllers/services/domain/views/tests). Persistence is GORM/Hibernate 4 over PostgreSQL, MySQL, or MariaDB (env-selected); search is Solr 5.4.1 (with an OmicsDI-backed alternative strategy); Redis is the shared cache/pub-sub/session backbone; Git (via JGit) is the versioned model-file store; Quartz drives scheduled maintenance/indexing; optional JMS/ActiveMQ remoting exists but is disabled by default.

## 2. Core app (`grails-app/`)

| Dir | Contents |
|---|---|
| `conf/` | `UrlMappings.groovy`, `BootStrap.groovy`, `BuildConfig.groovy`, `Config.groovy`, `DataSource.groovy`, `QuartzConfig.groovy`, request filters (`VerifyOtpFilters`, `ModelFilters`, `BrandingFilters`, `AdminHeaderFilters`, `ParameterFilters`, `PreProcessFilters`, `RequestLoggingFilters`, `SetupFilters`), plus embedded Solr core config (`conf/solr/conf/`) and OmicsDI schema |
| `controllers/` | Only 6 classes: `MaintenanceController`, `TestingController`, `UsermanagementController`, `healthcheck.HealthCheckController`, `security.{AuthController,LoginController,LogoutController}` (2FA + session flows). The bulk of MVC controllers live in `jummp-plugin-web-application` instead (§4). |
| `domain/` | Only 4 classes: `Feedback`, `model.ModelAudit`, `model.ModelHistoryItem`, `security.TwoFactorAuth`. **The "central" domain classes are not here** — see §3. |
| `jobs/` | 12 Quartz jobs (§5) |
| `migrations/` | 110 Grails Database Migration changelogs, 2014→present |
| `routes/` | `ExportingOmicsDIRoute`, `IndexingRoute`, `NotificationRoute` (Grails routes DSL, distinct from `UrlMappings.groovy`) |
| `services/` | 23 services, grouped: model lifecycle (`ModelService`, `ModelDelegateService`, `ModelFileFormatService`, `ModelHistoryService`, `ModelFlagService`, `SubmissionService`), metadata/publication (`MetadataService`, `MetadataDelegateService`, `PublicationService`, `PubMedService`, `DoiService`, `QcInfoDelegateService`), search (`SearchService`, `MiriamService`), auth/user (`AuthService`, `AuthenticationHashService`, `UserService`, `ReviewerAccountService`), VCS (`VcsService`), infra (`FileSystemService`, `LocaleService`, `MailingService`, `NotificationService`, `FeedbackService`), healthcheck |
| `utils/` | `DisplayFormat`, `EmailUtils`, `InputParameterSanitizer` |

### Routing (`UrlMappings.groovy`)
Dedicated model-workflow routes (`/model/submit|create|update|uploadFile|publish|...`), a generic `/model/$action/$id(.$revisionId)?` pattern (mirrored for `/contributor/...`), a bare `/$id(.$revisionId)?` → `ModelController.show` validated against a regex assembled dynamically from the registered model-identifier generators (supports multiple ID schemes, e.g. perennial IDs), a large `/api/*` block (model CRUD, OMEX generation, metadata/annotations, submission, curation notes, contributor management, search index, Redis-cached homepage widgets), plus static/marketing pages, auth/2FA routes, CMS routes, and yearly "model of the year" competition routes.

### Startup (`BootStrap.groovy`)
Seeds roles (`ROLE_USER/CURATOR/ADMIN/QC_PROVIDER/REVIEWER`) and an `administrator`/locked `anonymous` user, contribution roles (Curator/Modeller/Other), publication link providers (only PubMed/DOI/Custom/Manual are active — ArXiv/ISBN/ISSN/JSTOR/NARCIS/NBN/PMC are commented out pending annotation-UI fixes), registers the `UNKNOWN` model format, re-applies GORM validation to all domain classes (a documented workaround), strips the GSON converter from `RestBuilder`, and starts a Redis pub/sub subscriber for model-identifier generation (torn down on `destroy`).

## 3. Domain model — the central classes live in an external artifact, `AnnotationStore`

`CLAUDE.md` states that `Model`, `Revision`, `Publication`, `Person`, `Team`, `RepositoryFile` are GORM domain classes under `grails-app/domain/net/biomodels/jummp/`. **This is only true for `Team`, and `Team` actually lives in `jummp-plugin-web-application`, not `grails-app/domain`.** The rest are real GORM `@Entity` classes, but their source lives in a separate sibling repository, not in this checkout.

What actually happened (confirmed via git log and by reading the external repo):
- **2015-05-05, commit `3e92bbf70`**: `Model`, `ModelFormat`, `Publication`, `PublicationLinkProvider`, `RepositoryFile`, `Revision` were deleted from `grails-app/domain/net/biomodels/jummp/model/` and moved into a separate Maven project ("for ease of integration with solrindexer"). In their place, `jummp-plugin-core-api/src/groovy/.../core/adapters/` gained wrapper classes (`ModelAdapter`, `RevisionAdapter`, `PublicationAdapter`, `PersonAdapter`, `ModelFormatAdapter`, `PublicationLinkProviderAdapter`, `DomainAdapter`, `DomainObjectComposite`) that convert the externally-sourced domain objects into `*TransportCommand` DTOs used across the app.
- **Earlier commit `6d2882caa`**: `Person`/`User`/`Role`/`UserRole` were similarly moved out, ultimately into the same external project.
- **2019-05-31, commit `2e9bf35e5`** ("JBM-350 ... resolved circular dependencies"): the local `jummp-plugin-security` plugin (which by then held only wiring, not these domain classes) was deleted and merged into `jummp-plugin-web-application` — only `Team`/`UserTeam` and their controller/service survived as local domain source.
- **The external project is `AnnotationStore`** (repo root `groupId: net.biomodels.jummp`, `artifactId: annotationstore` — lowercase since JBM-717, because GitHub Packages rejects uppercase; the Java packages did not change, checked out locally as a sibling at `../annotationstore` relative to this repo). It is a plain Maven/Groovy project (no Grails plugin descriptor) containing `@Entity`-annotated GORM domain classes under `src/main/groovy/net/biomodels/jummp/{model,plugins.security}/`. `grails-app/conf/BuildConfig.groovy` declares `compile "net.biomodels.jummp:annotationstore:${jummpDependencyVersions['annotationStore.version']}"` — this is the dependency that supplies them, not an unrelated annotation library as an earlier draft of this document assumed. The version lives once in `jummpDependencyVersions.properties`, which `jummp-plugin-configuration` and `jummp-plugin-core-api` read as well (previously three hand-synced copies, flagged by a "remember to update this setting" comment). The artifact is published to GitHub Packages (`ebi-biomodels/repository-archives`), so a build resolving it from the registry needs `JUMMP_ARTIFACTORY_URL` plus `GITHUB_USERNAME`/`GITHUB_READ_ACCESS_TOKEN`. It is treated as a first-class internal dependency, versioned and released like the rest of the app (its own git history shows JBM-ticket-driven commits, e.g. `4fba768 JBM-647 for BioStudies submission`, up to `1e7dc7e release 0.3.6`).

**Practical implication**: to read or modify `Model`/`Revision`/`Publication`/`Person`/`User`/`Role`/`RepositoryFile`/`ModelFormat`/`PublicationLinkProvider` field definitions, constraints, or relationships, open the `../annotationstore` sibling checkout — not this repo. Changes to those classes require cutting a new release of that project and bumping `annotationStore.version` in `jummpDependencyVersions.properties` (which covers the app and both plugins), plus the pinned version in `../biomodels-indexer/pom.xml`.

### Key classes in `AnnotationStore` (`net.biomodels.jummp.model` / `net.biomodels.jummp.plugins.security`)
- **`Model`** — `hasMany: [revisions: Revision, flags: ModelFlag]`; `vcsIdentifier` (unique, non-null path segment), `publication`, `deleted`, `submissionId`/`publicationId` (perennial IDs), `firstPublished`, `modellingApproach`, `otherInfo`, `isMetadataSubmission`. Docstring explicitly warns: never access `revisions` directly — it circumvents ACL, use `ModelService.getAllRevisions()`.
- **`Revision`** — `belongsTo: [model: Model]`, `hasMany: [repoFiles: RepositoryFile]`; `vcsId` (VCS commit hash, unique per model), `revisionNumber`, `owner: User`, `minorRevision`, `validated`, `name`/`description` (mutable per-revision, since a model's metadata can change between revisions), `format: ModelFormat`, `state: ModelState` (default `UNPUBLISHED`), `curationState: CurationState` (default `NON_CURATED`), `validationLevel: ValidationState` (default `APPROVE`), `qcInfo: QcInfo`, `deleted`.
- **`Publication`** — `hasMany: [models: Model]`; `journal`, `title` (required), `affiliation`, `synopsis`, `year`/`month`/`day`, `volume`/`issue`/`pages`, `linkProvider: PublicationLinkProvider`, `link`.
- **`PublicationLinkProvider`** — `LinkType` enum (`PUBMED`, `DOI`, `CUSTOM`, `MANUAL_ENTRY` active; `ARXIV`/`ISBN`/`ISSN`/`JSTOR`/`NARCIS`/`NBN`/`PMC` commented out "until we fix the integration with the annotation UI" — matches the disabled providers noted in `BootStrap.groovy`), `pattern` (regex for the identifier), `identifiersPrefix` (identifiers.org prefix).
- **`RepositoryFile`** — `belongsTo: [revision: Revision]`; `path`, `description`, `hidden`, `mainFile` (the canonical submission entry, e.g. the PharmML file for DDMoRe submissions), `userSubmitted`, `mimeType`.
- **`ModelFormat`** — `hasMany: [modelElementTypes: ModelElementType]`; `identifier` (machine-readable, e.g. `SBML`), `name` (human-readable), `formatVersion` (defaults to `"*"` via `beforeInsert`/`beforeUpdate`) — designed so plugins can register new formats at runtime without touching the core app.
- **`User`** (`plugins.security`) — Spring Security Core-generated shape: `username`, `password`, `person: Person`, `email`, `enabled`/`accountExpired`/`accountLocked`/`passwordExpired`, plus custom additions `registrationCode`/`registrationInvalidation`, `passwordForgottenCode`/`passwordForgottenInvalidation`. `getAuthorities()` queries `UserRole` directly (HQL). `sanitizedUser()` strips security-sensitive fields for safe display.
- **`Person`** — `userRealName`, `institution`, `orcid` (validated against the ORCID regex `\d{4}-\d{4}-\d{4}-\d{3}(\d|X)`).
- **`Role`** — single `authority` string field (unique).
- **`UserRole`** — composite-key (`role`+`user`) join entity between `User` and `Role`, with standard `get`/`create`/`remove`/`removeAll` static helpers (Spring Security Core convention).

Domain classes present locally in this repo and readable directly:
- `grails-app/domain/.../Feedback.groovy` — star rating (1–5) + optional email/comment
- `grails-app/domain/.../model/ModelAudit.groovy` — model/user/AccessFormat/AccessType/changesMade/dateCreated/success
- `grails-app/domain/.../model/ModelHistoryItem.groovy` — composite model+user PK, `lastAccessedDate`
- `grails-app/domain/.../security/TwoFactorAuth.groovy` — `otp`, `issuedDate`, `sessionId`, `belongsTo User`
- `jummp-plugin-web-application/.../plugins/security/{Team,UserTeam}.groovy`
- `jummp-plugin-web-application/.../webapp/{Notification,NotificationUser,NotificationTypePreferences,Preferences,RequestType}.groovy`
- `jummp-plugin-simple-cms/.../scms/CmsContent.groovy`
- `jummp-plugin-biomodels-dom/.../deployment/biomodels/{AutoGeneratedCategory,CurationNotes,ModelClass,ModelOfTheMonth,P2MMapping,UhlenModelMapping}.groovy`

## 4. Plugin inventory

Load order is declared per-plugin via `loadAfter = [...]` in each `*GrailsPlugin.groovy` (this — not the stale `docs/plugin-dependency.dot` — is the source of truth):

| Plugin | Depends on | Purpose |
|---|---|---|
| `jummp-plugin-core-api` | — (foundation) | Shared interfaces/DTOs (`IModelService`, `IUserService`, `IPublicationService`, `ISbmlService`, `IPharmMlService`, ...), transport commands, model-identifier generator framework, domain adapters (§3), Redis utils, REST token services |
| `jummp-plugin-configuration` | core-api | First-run/externalized config — `ConfigurationController/Service`, `SetupController`, optional LDAP config (`LdapCommand`) |
| `jummp-plugin-git` | core-api, configuration | Git-backed VCS via JGit (`GitManager`, `GitSupport`) — no shell-out to `git` |
| `jummp-plugin-subversion` | core-api | Legacy SVN backend — **disabled** in root `BuildConfig.groovy` |
| `jummp-plugin-sbml` | configuration | SBML parsing/validation (JSBML) |
| `jummp-plugin-combine-archive` | core-api | COMBINE Archive / OMEX support |
| `jummp-plugin-common-format` | — | Non-SBML format detection: C/C++, Python, R, Java, Mathematica, MATLAB, ONNX, BMS, CellML3D, MorpheusML |
| `jummp-plugin-pharmml` | core-api | PharmML format support |
| `jummp-plugin-mdl` | pharmml | MDL (Model Description Language) format support |
| `jummp-plugin-bives` | core-api | Model diffing (BiVeS) |
| `jummp-plugin-qc-info` | — | Model QC/certification metadata |
| `jummp-plugin-annotation-core` | core-api, configuration | Annotation infrastructure (RDF/SBML writers) |
| `jummp-plugin-annotation-source-ddmore` | core-api, configuration, annotation-core, web-application | DDMoRe SPARQL metadata integration — **not wired into the build** (commented out) |
| `jummp-plugin-biomodels-dom` | core-api | BioModels-specific domain: Model-of-the-Month, classification, curation notes, P2M mapping, Uhlen mapping, GO chart, competitions, parameter search, home page — largest plugin (13 controllers, 14 services) |
| `jummp-plugin-omicsdi` | — | OmicsDI external search-index integration |
| `jummp-plugin-simple-cms` | core-api | In-house CMS (root `weceem/` dir suggests it superseded the Weceem CMS plugin) |
| `jummp-plugin-simple-logging` | core-api | Logging infra (no controllers/services — AOP/log config only) |
| `jummp-plugin-web-application` | core-api | Main MVC web layer (§6); also now hosts the former `jummp-plugin-security` content (`Team`, `Contributor*`) |
| `jummp-plugin-remote` | core-api | Remoting abstraction, base for JMS |
| `jummp-plugin-jms` | core-api, remote, sbml, bives | JMS adapters per domain area |
| `jummp-plugin-jms-remote` | core-api, remote, sbml, bives, jms | Synchronous request/reply over JMS — **not wired into the build** |
| `jummp-plugin-ast` | — | Builds/loads `AST.jar`, supports JMS remoting |

**Conditional activation**: `jummp-plugin-ast`/`remote`/`jms` are only registered when env var `JUMMP_EXPORT=jms` is set; `jummp-plugin-jms-remote`, `jummp-plugin-annotation-source-ddmore`, and `jummp-plugin-subversion` are permanently commented out of `grails.plugin.location`. **In a default build, only 16 of the 21 plugin directories are actually active.**

### Where Spring Security / 2FA actually lives
There is **no `jummp-plugin-security` plugin** — it was deleted in 2019 (§3). Configuration lives in `grails-app/conf/Config.groovy` (`userDomainClassName`/`authorityJoinClassName`/`authority.className` pointing at `net.biomodels.jummp.plugins.security.*`, `securityConfigType = "Annotation"`, REST login, `password.algorithm = SHA-256`, CORS on `/api/*`). 2FA-specific pieces:
- Domain: `grails-app/domain/.../security/TwoFactorAuth.groovy`
- Controller: `grails-app/controllers/.../security/AuthController.groovy` (`load2fa`, `enrollTwoFactor`, `generateOTP`, `verifyOTP`, `checkTrustDevice`, `toggle2FA`)
- Filter: `grails-app/conf/VerifyOtpFilters.groovy` — gates every request behind 2FA unless `@Secured(permitAll/IS_AUTHENTICATED_ANONYMOUSLY)` or explicitly allowlisted
- Lockout: `UnlockAccountJob`, `AuthenticationOutdaterJob`, `RegistrationCheckerJob` jobs + `core-api`'s Redis-backed `LoginAttemptCacheService`

## 5. Build, config, data

**`BuildConfig.groovy`**: Maven resolver, groupId `net.biomodels.jummp`, Java 8. Repos: optional private Artifactory (`JUMMP_ARTIFACTORY_URL`), Grails/Maven Central, two EBI-hosted repos, Spring milestones, Grails core, JCenter. Notable deps: `solr-solrj:5.4.1`, `mysql-connector-java:8.0.11`, `mariadb-java-client:2.7.12`, `postgresql:9.1-901.jdbc4`, `ddi-ebe-ws-dao` (OmicsDI), Jena `jena-tdb/core/arq:3.17.0` (RDF store), `libCombineArchive`/`CombineExt`, `spring-data-redis`+`jedis`, `spring-security-{acl,core,ldap,rest}`, `database-migration:1.4.1`, `hibernate4:4.3.10`.

**`Config.groovy`**: Spring Security + REST login/logout, CORS, MIME types, GSP/Sitemesh, EhCache, per-environment `serverURL`, Log4j config, `jummp.metadata.strategy` (biomodels/ddmore/default), branding (`jummp.branding.deployment/style` — drives the `main.gsp` vs plugin-specific layout choice), declarative `@Secured` static rules, VCS paths, Redis connection settings, `jummp.search.strategy` (solr vs omicsdi).

**`DataSource.groovy`**: reads an external properties file and switches on `jummp.database.type` — `POSTGRESQL`, `MYSQL`, or `MARIADB` (added recently; `MySQL5InnoDBDialect` shared with MySQL). Falls back to embedded H2 (`jdbc:h2:mem:testDb`) in `test` env or when no config file is found.

## 6. External integrations

- **Solr** — `src/groovy/.../search/{SolrBasedSearch,SolrServerHolder}.groovy`; schema under `grails-app/conf/solr/conf/`; reindexed by `SearchIndexDbJob`/`SearchIndexOptimiserJob`.
- **Redis** — `jummp-plugin-core-api`'s `RedisService`/`PublishClient`/`SubscribeClient`; used by auth, model, search, 2FA/lockout caching, and `biomodels-dom` homepage features; also backs Spring Session and the model-identifier pub/sub started in `BootStrap`.
- **Git (JGit)** — `jummp-plugin-git`; no shell-outs, pure Java API.
- **JMS/ActiveMQ** — `jummp-plugin-jms`/`jms-remote`; disabled unless `JUMMP_EXPORT=jms`.
- **LDAP** — optional, via `spring-security-ldap` + `jummp-plugin-configuration`'s `LdapCommand`; DB-backed Spring Security is the default.
- **OAuth** — not present anywhere in the codebase.
- **OmicsDI** — `jummp-plugin-omicsdi`, alternative search backend to Solr.

### Quartz jobs (`grails-app/jobs/.../core/`)
`AccessTokenExpirationCheckerJob` (30 min), `AuthenticationOutdaterJob` (15 min), `GarbageCollectionBasedCleanerJob` (3 min), `HomePageUpdaterJob` (daily 02:00), `MiriamRegistryExportUpdaterJob` (weekly Sat 02:15), `OldFilesExchangeCleanerJob` (6 hr), `RefreshModelIdentifiersOnRedisJob` (daily 02:00), `RefreshSearchCacheJob` (daily 10:30), `RegistrationCheckerJob` (5 min), `SearchIndexDbJob` (daily 23:05, full reindex), `SearchIndexOptimiserJob` (daily 02:00), `UnlockAccountJob` (account-lockout expiry), `UpdateCachedParametersOnRedisJob` (weekly Tue 10:00). Quartz itself: RAM job store, 10-thread pool, disabled in `test` env.

## 7. Web layer (`jummp-plugin-web-application`)

18 controllers: `SystemController`, `CommonController`, `SubmissionController`, `JummpController`, `NotificationController`, `ErrorsController`, `ModelController`, `SearchController`, `TermsController`, `PostController`, `CurationController`, `ConversionController`, `PublicationController`, `ContributorController`, `TeamController`, `AdminController`, `UserAdministrationController`, `CuratorController`, `ShowRestApiController`. Two services: `TeamService`, `ContributorService`. Views organized per-feature (`model/`, `search/`, `admin/`, `curator/`, `team/`, `contributor/`, `publication/`, `notification/`, `sbml/`, `annotation/`, `gotree/`, `errors/`, `templates/`, `showRestApi/`, `system/`).

Layouts (already documented in `CLAUDE.md`, confirmed): `grails-app/views/layouts/biomodels/main.gsp` (primary, has 2FA banner, used almost everywhere) vs `newmain.gsp` (legacy, only used by `display.gsp`, no 2FA banner).

## 8. Testing

- Root `test/unit` + `test/integration` (63 files): mix of legacy `GrailsUnitTestCase` classes and Spock specs; integration tests exercise JGit directly for VCS setup/teardown. Fixtures in `test/files/` (sample SBML/PharmML models, including deliberately invalid ones).
- Each plugin has its own `test/unit`/`test/integration`, run via its own `./grailsw`. Test coverage is uneven: `biomodels-dom` has 35 test files (most), `pharmml` 21, `common-format` 18, `sbml` 16; **zero tests** in `annotation-core`, `annotation-source-ddmore`, `ast`, `bives`, `jms`, `jms-remote`, `remote`, `simple-logging`.
- Spock HTML reporting via `spock-reports:1.3.0` (test-scope only).

## 9. Notable architectural risks / observations

1. **Core domain layer lives in a separate repository (`AnnotationStore`, sibling checkout `../annotationstore`)**: `Model`, `Revision`, `Publication`, `Person`, `User`, `Role`, `RepositoryFile`, `ModelFormat`, `PublicationLinkProvider` have no source in this repository (§3) — they're pulled in via the `net.biomodels.jummp:annotationstore` Maven dependency declared in `BuildConfig.groovy` (version in `jummpDependencyVersions.properties`). This was the single biggest gap between what the docs claimed and what's checked in here; any onboarding or AI-assisted work on model/user persistence needs to open the sibling `annotationstore` checkout directly, and any schema change requires a coordinated version bump across `BuildConfig.groovy`, `jummp-plugin-configuration`, and `jummp-plugin-core-api`.
2. **Stale dependency graph**: `docs/plugin-dependency.dot` still models a `jummp-plugin-security` node removed in 2019 — worth regenerating or deleting.
3. **Dead/dormant plugins in the tree but not the build**: `jummp-plugin-subversion`, `jummp-plugin-jms-remote`, `jummp-plugin-annotation-source-ddmore` are present as source but excluded from `grails.plugin.location`; `jms`/`remote`/`ast` are gated behind an env var. This inflates the apparent plugin count (23 dirs) vs. what's actually active (16).
4. **Two search backends** (Solr vs OmicsDI) and **three DB dialects** (Postgres/MySQL/MariaDB) are config-switched rather than abstracted behind separate deployable profiles — increases `Config.groovy`/`DataSource.groovy` branching complexity.
5. **Uneven test coverage** concentrated in format-handling plugins (`biomodels-dom`, `pharmml`, `common-format`, `sbml`); the messaging/remoting/annotation-DDMoRe plugins have none, consistent with them being effectively dormant.
