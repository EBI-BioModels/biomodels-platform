# 03 — Risk Register

Scoring: **Likelihood** and **Impact** each Low / Med / High. "Exposure" = rough product.
Risks are grouped; within a group they are ordered by exposure.

---

## A. Framework / build discontinuity

### A1 — No supported upgrade path; effectively a rewrite
- **Likelihood: High · Impact: High · Exposure: Critical**
- Grails 2→3 replaced build, runtime, config, plugin model and request pipeline; 2→7 also
  adds the Jakarta jump. There is no `grails upgrade` from 2.5.5.
- **Consequences:** effort is measured in engineer-quarters, not weeks; a "big bang" cutover
  is high-risk; partial migration leaves two incompatible build systems in the tree.
- **Mitigation:** treat as a re-platform onto a fresh Grails skeleton (strangler pattern);
  freeze the plugin structure early; migrate leaf plugins first, `web-application` last;
  keep the Grails 2 app releasable on a maintenance branch throughout.

### A2 — Build/dependency resolution rewrite (`BuildConfig.groovy` → Gradle)
- **Likelihood: High · Impact: Med · Exposure: High**
- ~40 dependencies with hand-tuned `excludes` (jena, activemq, spring-data-redis,
  spring-context clashes, xstream compile-scope hack, GSON removal). Gradle resolves
  transitive deps differently (nearest-wins vs highest-wins); the exclusion set will not
  translate 1:1 and new conflicts will appear.
- **Mitigation:** rebuild the dependency graph from first principles, not by porting
  excludes; use `gradle dependencies` / dependency-locking; pin the internal artifacts.

### A3 — 128/131 test files use removed test APIs
- **Likelihood: High (certain) · Impact: High · Exposure: Critical**
- `grails.test.mixin`, `GrailsUnitTestCase`, `@TestFor` mixin removed in Grails 3.3. Until
  rewritten to `grails-testing-support`, **there is no regression safety net** while the
  domain layer, ORM and request pipeline are all changing.
- **Mitigation:** port the highest-value specs (auth, submission, model lifecycle, VCS,
  format detection) **first**, before touching production code; add characterization tests
  against the running Grails 2 app (HTTP-level) to lock current behaviour.

---

## B. Dead-end plugins (no Grails 7 equivalent)

### B1 — `routing` plugin (Apache Camel DSL) abandoned
- **Likelihood: High · Impact: High · Exposure: Critical**
- No Grails 3+ release. Drives `ExportingOmicsDIRoute`, `IndexingRoute`, `NotificationRoute`
  — OmicsDI export, Solr (re)indexing hand-off, and user notification delivery.
- **Mitigation:** re-implement as a plain Spring-managed `CamelContext` (Camel 4 runs fine
  on Spring Boot 3), or replace the three routes with service methods triggered by Quartz /
  Redis pub-sub / Spring events. Estimate each route as a small standalone design task.

### B2 — WebFlow first-run wizard
- **Likelihood: Med · Impact: Med · Exposure: Med-High**
- `webflow:2.1.0` → `grails-webflow` exists but is minimally maintained and the flow DSL /
  continuation handling changed. `SetupController` + ~49 GSPs in `jummp-plugin-configuration`.
- **Mitigation:** the setup wizard runs once per deployment — replace it with a linear
  multi-step controller + session-scoped command object; drop WebFlow entirely.

### B3 — `simple-captcha`, `perf4j`, `webxml`, `locale-variant`, `cache-ehcache`
- **Likelihood: High · Impact: Med · Exposure: High**
- None have current releases.
  - `simple-captcha` → reCAPTCHA/hCaptcha integration or a maintained captcha lib (registration + feedback forms).
  - `perf4j` (dead since 2011, Log4j-1 based) → Micrometer / Spring Boot Actuator metrics.
  - `webxml` (web.xml mutation) → Spring Boot has no `web.xml`; re-express as
    `WebMvcConfigurer` / `FilterRegistrationBean` / servlet-init beans.
  - `locale-variant` (locale-suffixed GSPs) → Grails i18n + custom `LocaleResolver`, or
    fold variants into the views.
  - `cache-ehcache` → Grails cache + Caffeine (Spring Boot 3 default) or JCache/Ehcache 3.
- **Mitigation:** each is a self-contained replacement task; do them as prerequisites, not
  during the big port.

### B4 — `console` plugin, `scaffold-core`, `codenarc:1.2`
- **Likelihood: Med · Impact: Low · Exposure: Low-Med**
- `console` has G3+ versions; `scaffold` is built-in now (check if still used);
  `codenarc:1.2` → CodeNarc 3.x Gradle plugin (rule set changes will flag many warnings).

---

## C. Domain model / persistence

### C1 — `AnnotationStore` must be re-cut against GORM 9 / Hibernate 6 first
- **Likelihood: High · Impact: High · Exposure: Critical**
- Core entities (`Model`, `Revision`, `Publication`, `RepositoryFile`, `ModelFormat`,
  `PublicationLinkProvider`, `User`, `Role`, `UserRole`, `Person`, `AuthToken`) live in the
  separately-released `AnnotationStore` artifact (currently Groovy 2.4.4,
  `grails-datastore-gorm-hibernate4:3.1.5`). It is consumed by both this app **and**
  `biomodels-indexer`.
- **Consequences:** nothing in the main app compiles until AnnotationStore ships a
  GORM-9-compatible release; a mistake here corrupts the model repository.
- **Mitigation:** migrate AnnotationStore as **phase 0** — convert it to a Grails 7 GORM
  library (or a proper plugin), `jakarta.persistence`, GORM 9 mappings; validate with a
  read-only integration harness against a production DB snapshot before the app depends on it.
  Coordinate the version-bump-in-three-places dance (`BuildConfig`/`build.gradle`,
  `jummp-plugin-configuration`, `jummp-plugin-core-api`).

### C2 — Hibernate 4 → 6 query/mapping behaviour changes
- **Likelihood: High · Impact: High · Exposure: Critical**
- Legacy Criteria API removed; HQL stricter; implicit-join, pagination, `null` collection,
  cascade and flush-order semantics changed. 14 `createCriteria`/`withCriteria` + 19
  `executeQuery`/HQL sites, plus GORM dynamic finders throughout 94 services.
- **Consequences:** silent wrong results (wrong model revision shown, ACL bypass via
  `Model.revisions` direct access which the code already warns about, search index fed stale
  rows), or `LazyInitializationException` storms.
- **Mitigation:** enable Hibernate SQL logging in a staging run and diff query output
  old-vs-new for the hot paths (model show, search reindex, submission commit); add
  integration tests around every custom criteria/HQL query before changing it.

### C3 — Database-migration changelog baseline drift
- **Likelihood: Med · Impact: High · Exposure: High**
- 110 changelogs, plugin `1.4.1` → Liquibase 4. Checksum algorithm and some change types
  differ; production schema may have drifted from the changelog history over 12 years.
- **Mitigation:** `liquibase diff` production schema vs a fresh changelog-built schema;
  generate a new baseline changelog with `changelog-sync`; never let the migrated app run
  `dropAll`/`update` against production without a dry run.

### C4 — Three DB dialects (Postgres / MySQL / MariaDB) each need re-validation
- **Likelihood: Med · Impact: Med · Exposure: Med**
- `DataSource.groovy` switches on `jummp.database.type`. Hibernate 6 dialect auto-detection
  and the removal of version-specific dialect classes change generated DDL/SQL per engine.
- **Mitigation:** decide whether all three are still supported in production; if only one is,
  drop the others from the migration scope and test matrix.

---

## D. Security (highest user-facing blast radius)

### D1 — Password hash migration (`SHA-256`, 1 iteration)
- **Likelihood: High · Impact: High · Exposure: Critical**
- Modern `spring-security-core` plugin defaults to bcrypt via a `DelegatingPasswordEncoder`.
  If the encoder config is not carried forward precisely, **every existing user fails login
  on cutover**.
- **Mitigation:** configure a delegating encoder that still recognises the legacy
  `{sha256}`-style hashes (custom `PasswordEncoder` prefixing), and transparently re-hash to
  bcrypt on next successful login. Also: this is the moment to retire 1-iteration SHA-256 —
  security review should sign off the transition plan.

### D2 — 2FA / lockout / breach-check stack regression (JBM-686/689, live since 2026-05-04)
- **Likelihood: High · Impact: High · Exposure: Critical**
- Built on Grails 2 Filters (`VerifyOtpFilters`), `spring-security-core:2.0.0` +
  `spring-security-rest:1.5.3`, Redis (`LoginAttemptCacheService`), and 3 jobs. All of:
  filter→interceptor rewrite, plugin major-version bump, jedis API change, Quartz job API
  change land on this feature at once.
- **Mitigation:** treat auth as its own migration workstream with a dedicated test plan
  (OTP issue/verify/expire, 30-day device trust, 3-strike lockout, 1-hour unlock,
  registration expiry); run old and new side-by-side against the same Redis; involve
  security review before cutover. This feature is <6 months old and mandatory — regressions
  are an incident, not a bug.

### D3 — `spring-security-rest` token chain + custom `filterChain.chainMap`
- **Likelihood: High · Impact: Med · Exposure: High**
- The hand-crafted stateless chain for `/api/**`
  (`-exceptionTranslationFilter,-authenticationProcessingFilter,…`) references internal
  filter bean names that were renamed across plugin versions. `spring-security-rest` 3.x
  restructured token storage/validation.
- **Mitigation:** rebuild the API security chain from the 3.x plugin's documented model
  rather than porting the filter-subtraction list; contract-test every `/api/*` endpoint
  (the API is consumed by external clients — `biomodels-python-restful-client`,
  `biomodels-python-api-demo`, third parties).

### D4 — Session fixation / CSRF / cookie defaults changed
- **Likelihood: Med · Impact: Med · Exposure: Med**
- Config explicitly disables session-fixation prevention and tunes session creation for
  stateless paths. Spring Security 6 changed defaults (CSRF, `SameSite`, session-fixation
  strategy, `requestMatchers` API, `authorizeHttpRequests`).
- **Mitigation:** re-derive each `grails.plugin.springsecurity.*` setting against the 6.x
  property names (many renamed/removed); pen-test the migrated login + API flows.

### D5 — `spring-session:1.2` Redis session store
- **Likelihood: Med · Impact: Med · Exposure: Med**
- Spring Session 1.2 → 3.x; serialization format and namespace changed. Rolling deploy with
  a shared Redis means old and new nodes may not read each other's sessions → users logged
  out mid-migration.
- **Mitigation:** plan a session-flush maintenance window at cutover; announce it (there is
  precedent — the 2026 security rollout was emailed to all users).

---

## E. Views / front-end

### E1 — 455 GSPs across SiteMesh 2 → 3 + asset-pipeline
- **Likelihood: High · Impact: Med · Exposure: High**
- SiteMesh 3 changes decorator resolution and some `<g:*>`/`<sitemesh:*>` behaviour; the
  `resources` plugin's `<r:*>` tags have no equivalent and every bundle must be redeclared
  under `grails-app/assets/`. 2,181 JS files to reorganise; `newmain.gsp` (legacy) and
  `biomodels/main.gsp` (2FA banner, contextual help) both need careful re-verification.
- **Mitigation:** inventory `<r:require>` modules and the `ApplicationResources.groovy`
  config; migrate to asset-pipeline `require_tree`/manifest files; visual-diff key pages
  (home, model show, search, submission wizard, admin) old-vs-new; keep the contextual-help
  build (`jummp-contextual-help` sibling) in sync.

### E2 — 14 tag libraries + custom namespaces
- **Likelihood: Med · Impact: Low-Med · Exposure: Med**
- TagLib API is largely stable 2→3, but `out <<`, `throwTagError`, namespace clashes and
  `@CompileStatic` strictness can bite. `ScmsTagLib`, `SbmlTagLib`, `PharmMlTagLib`,
  `MdlTagLib`, `BioModelsParametersTagLib`, `AnnotationTagLib`, `ContextHelpTagLib`, etc.
- **Mitigation:** unit-test each taglib (these port relatively cleanly to
  `TagLibUnitTest`); run the full view render smoke suite.

---

## F. External systems / infrastructure

### F1 — Solr 5.4.1 server upgrade forced by SolrJ bump
- **Likelihood: High · Impact: High · Exposure: High**
- SolrJ 8/9 ↔ Solr 8/9. Embedded core config lives in `grails-app/conf/solr/conf/`; schema
  needs migration to managed schema; `solrconfig.xml` API/handler changes; **full reindex of
  2,700+ models** and re-tuning of faceting/relevancy.
- **Mitigation:** stand up the new Solr in parallel; port `schema.xml`; reindex from the DB
  via the existing `SearchIndexDbJob` path (once ported); A/B search-result quality before
  switching `jummp.search.strategy`.

### F2 — `biomodels-indexer` shares the domain artifact and GORM stack
- **Likelihood: High · Impact: High · Exposure: High**
- Maven project, Spring Boot + `grails-datastore-gorm-hibernate4` + `groovy-all` 2.4, depends
  on AnnotationStore entities. It cannot keep using the old AnnotationStore once the app's
  copy is re-cut (same Maven coordinate/version).
- **Mitigation:** migrate indexer immediately after AnnotationStore, before the app; align
  its Groovy/GORM/Spring-Boot versions; this is a good candidate to move to GORM-standalone
  on Spring Boot 3 (it is already Spring-Boot-shaped).

### F3 — Jena 3.17 → 4/5 (RDF metadata store)
- **Likelihood: Med · Impact: Med · Exposure: Med**
- Jena 4 needs Java 11+, Jena 5 needs Java 17+; `jena-tdb` → `jena-tdb2`; some API moves.
  Used by the annotation/metadata path.
- **Mitigation:** bump alongside the Java jump; test TDB read/write and SPARQL on the
  metadata service.

### F4 — Redis / jedis / spring-data-redis triple bump
- **Likelihood: High · Impact: Med · Exposure: Med-High**
- `jedis` 2.9 → 5.x is a breaking API change; the in-house `RedisService` /
  `PublishClient` / `SubscribeClient` in `core-api` and the `BootStrap` pub/sub subscriber
  use the raw jedis API.
- **Mitigation:** rewrite the Redis abstraction against jedis 5 or Lettuce; keep the
  wire protocol/key schema identical so a shared Redis works during rollout.

### F5 — Old JDBC drivers / TLS / container
- **Likelihood: Med · Impact: Med · Exposure: Med**
- `postgresql:9.1-901.jdbc4` is a decade old and won't behave well on Java 17 / PG 14+;
  `-noverify` and CMS GC flags break the JVM on 17; base images are EOL.
- **Mitigation:** current PG/MySQL/MariaDB drivers; rewrite `Dockerfile` on a supported JDK
  17/21 base + Tomcat 10.1 (or executable jar); update k8s manifests and `start.sh`.

### F6 — `jummp-plugin-ast` Groovy compiler-internal APIs
- **Likelihood: Med · Impact: Low · Exposure: Low** (JMS remoting is disabled by default)
- AST transformations against `org.codehaus.groovy.*` internals that moved in Groovy 4.
- **Mitigation:** decide whether JMS remoting is still wanted. If not, delete
  `ast`/`remote`/`jms`/`jms-remote` (4 plugins, ~0 tests, env-gated) and remove the risk
  entirely. If yes, rewrite the transforms for Groovy 4.

---

## G. Programme / people risks

### G1 — Feature freeze / double-maintenance
- **Likelihood: High · Impact: High · Exposure: Critical**
- A migration of this size runs multiple quarters. Either the JBM backlog stalls or every
  fix must be applied to both the Grails 2 maintenance branch and the migration branch.
- **Mitigation:** define an explicit freeze scope (security fixes only on the old branch);
  time-box the migration into shippable phases; keep the old app deployable until the new
  one passes an agreed acceptance suite.

### G2 — Shrinking Grails-2 / Groovy-2 skills pool
- **Likelihood: Med · Impact: Med · Exposure: Med**
- Fewer developers know the old stack each year; onboarding onto a dead framework is hard.
  This is an *argument for* doing the migration, but also a risk *during* it (limited people
  who understand the current wiring well enough to port it faithfully).
- **Mitigation:** capture behaviour in characterization tests now, while the knowledge
  exists; `docs/architecture-analysis.md` is a good start — extend it per plugin.

### G3 — Undocumented production config / operational scripts
- **Likelihood: Med · Impact: Med · Exposure: Med**
- Config is loaded from an external properties file not in the repo; 18 `scripts/*.groovy`
  operational tools depend on the Grails 2 script runner; CI is not in this repo.
- **Mitigation:** collect the real production config; port scripts to Gradle tasks or
  standalone mains; commit the CI pipeline alongside the new build.

### G4 — "Latest" is a moving target during the project
- **Likelihood: Med · Impact: Low · Exposure: Low**
- Grails 7 minors, Spring Boot 3.x, Groovy 4.x will all advance during a multi-quarter
  migration.
- **Mitigation:** pin a target (e.g. Grails 7.0.x, Spring Boot 3.5.x) at kickoff; treat
  further bumps as routine maintenance after landing.

---

## Risk heat summary

| Area | Worst-case exposure |
|---|---|
| A. Framework/build discontinuity | **Critical** |
| B. Dead-end plugins | **Critical** (B1 routing) |
| C. Domain/persistence | **Critical** (C1, C2) |
| D. Security | **Critical** (D1, D2) |
| E. Views/front-end | High |
| F. External systems | High |
| G. Programme/people | **Critical** (G1) |

No area is low-risk. The migration is dominated by four "must not get this wrong" items:
**AnnotationStore/GORM re-cut, password + 2FA continuity, the Camel routing replacement,
and keeping a regression signal (tests) alive throughout.**
