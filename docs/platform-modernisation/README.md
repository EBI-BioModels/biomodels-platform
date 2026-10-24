# BioModels / Jummp — Platform Modernisation: Feasibility & Risk Assessment

_Generated 2026-09-06 by reading the repository, the sibling in-house projects, and the
build configuration directly. Companion to `docs/architecture-analysis.md`._

## What this covers

The proposal is to modernise **infrastructure, codebase and the whole project onto the latest
version of the full-stack framework** — i.e. take BioModels from **Grails 2.5.5** (released
2016, end-of-life since 2017) to the **current Grails line (7.0, released 2025)**, together
with the JVM, the servlet container, Groovy, Hibernate/GORM, Spring, Spring Security and the
~40 third-party libraries the app pulls in — plus the coupled in-house projects
(`AnnotationStore`, `biomodels-indexer`, `CombineExt`, `libCombineArchive`, FileService, …).

## Documents in this set

| File | Contents |
|---|---|
| [`01-current-state.md`](01-current-state.md) | Exactly what is installed today: framework, JVM, plugins, libraries, code volume, build & deploy pipeline |
| [`02-target-and-gaps.md`](02-target-and-gaps.md) | What "latest" means (Grails 5 / 6 / 7), the version deltas, and the structural gaps that make this a re-platform rather than a version bump |
| [`03-risk-register.md`](03-risk-register.md) | Enumerated risks with likelihood / impact / mitigation — the core deliverable |
| [`04-dependency-and-plugin-matrix.md`](04-dependency-and-plugin-matrix.md) | Per-plugin and per-library migration verdict (survives / needs replacement / dead-end) |
| [`05-inhouse-projects.md`](05-inhouse-projects.md) | The team-owned dependencies that must migrate in lockstep, and the ordering constraints |
| [`06-migration-strategy.md`](06-migration-strategy.md) | Recommended approach, phasing, effort bands, and the lower-risk alternatives |
| [`07-decoupled-architecture-option.md`](07-decoupled-architecture-option.md) | Alternative: split into a Spring Boot API backend + React/TS frontend — key differences vs. the Grails 7 re-platform, effort, and HTTP-layer strangler sequencing |

## Executive summary

**This is not an upgrade; it is a re-platform.** Grails changed its entire foundation
between 2.x and 3.x (2016): the build system (custom → Gradle), the runtime (bespoke → Spring
Boot), configuration (`*.groovy` DSLs → YAML/profiles), the request pipeline (Filters →
Interceptors), the view/asset stack (SiteMesh 2 + `resources` plugin → SiteMesh 3 +
asset-pipeline), and the plugin model (in-place source plugins → binary Gradle plugins).
Grails 7 adds a second discontinuity: **Jakarta EE** (`javax.*` → `jakarta.*` across the
whole stack), Spring 6, Groovy 4, Hibernate 6, Java 17+. There is **no supported in-place
upgrade path** from 2.5.5; Grails' own guidance is to stand up a fresh application and port
code into it.

**Scale of the thing being moved:**

- ~115,000 lines of Groovy/Java, 999 Groovy source files, **455 GSP views**, 61 controllers,
  94 services, 14 tag libraries, **8 filter classes**, 3 Camel route definitions,
  13 Quartz jobs, 110 database-migration changelogs.
- **21 in-tree Grails plugins** (16 active), each with its own descriptor, build config and
  tests — every one needs restructuring to the Grails 7 plugin layout.
- **131 test files, 128 of which use `grails.test.mixin` / `GrailsUnitTestCase`** — an API
  deleted in Grails 3.3. The entire test suite needs rewriting before it can verify anything.
- The **core domain model is not in this repo** — `Model`, `Revision`, `Publication`,
  `User`, `Role`, etc. live in the separately-released `AnnotationStore` artifact (Groovy
  2.4.4, `grails-datastore-gorm-hibernate4` 3.1.5). It must be re-cut against GORM 9 first.
- Several **team-owned satellites** (`biomodels-indexer`, `CombineExt`, `libCombineArchive`,
  FileService, `biomodels-parameters`) share the domain classes and/or the Groovy/GORM/Java
  baseline and have to move together.

**Highest-severity risks** (full register in `03-risk-register.md`):

1. **Dead-end plugins with no Grails 7 equivalent** — `routing` (Apache Camel DSL, used for
   OmicsDI export + Solr indexing + notifications), `webflow` (the first-run setup wizard),
   `simple-captcha`, `perf4j`, `webxml`, `cache-ehcache`, `locale-variant`. Each needs a
   bespoke replacement, not a version bump.
2. **`AnnotationStore` / GORM re-cut** — GORM 4 → 9 and Hibernate 4 → 6 change mapping
   semantics, dirty-checking, and query behaviour; the domain artifact and every consumer
   (app + indexer) must be validated together, with a schema/data-migration safety net.
3. **Password hashing** — `SHA-256`, 1 iteration is configured. Modern Spring Security
   defaults to bcrypt/delegating encoders; a login-preserving migration path is required or
   every user is locked out on cutover.
4. **The 2FA / lockout / security rollout** (JBM-686/689, live since 04 May 2026) is built on
   Grails 2 Filters, the `spring-security-*` 2.0 plugins and Redis wiring — all of which
   change shape. Regression here is a user-facing security incident.
5. **Test suite is non-functional during the migration** — until 128 test files are ported,
   there is no automated regression signal for a codebase whose domain layer, request
   pipeline and persistence are all changing at once.
6. **Frozen feature development** — a migration of this size runs for many months; the JBM
   backlog either stalls or has to be double-maintained across two branches.

**Bottom line:** technically achievable and strategically worthwhile (the current stack has
unpatched CVEs and a shrinking pool of people who can maintain it), but it is a **major
multi-quarter programme**, not a sprint. `06-migration-strategy.md` recommends a
strangler-style re-platform onto a fresh Grails 7 skeleton, domain-artifact first, with an
explicit decision point on whether some satellites (indexer, converters, APIs) should leave
Grails altogether for plain Spring Boot / Micronaut.

## Effort & timeline comparison — the two candidate strategies

Two paths are on the table (details: `06` and `07`). Both must first do the same
**shared foundation work** — it is not a differentiator.

| Workstream | **A. Grails 7 full-stack re-platform** | **B. Decoupled: Spring Boot API + React/TS frontend** |
|---|---|---|
| AnnotationStore GORM 9 re-cut + indexer + native deps (`05`) | 1.5–3 | 1.5–3 (+0.5–1 if domain moves to JPA) |
| Backend services / Solr / Redis / Git / Quartz / Camel routes | 5–9 | 4–7 (no plugin restructuring, no GSP/asset work) |
| Complete + documented REST API (beyond today's partial `/api/*`) | — | 3–6 _(new, on critical path)_ |
| View layer | 4–8 (455 GSP → SiteMesh 3 + asset-pipeline) | 6–12 _(greenfield React rewrite; parallelisable)_ |
| Test rewrite (128 files) + new FE test setup | 2–4 | 2–4 backend + 1–2 frontend |
| Security workstream (password hash, 2FA, API chain) | 1.5–3 | 2–4 _(auth-flow redesign for SPA)_ |
| Infra / Solr 9 / cutover / soak | 1.5–3 | 2–4 _(two pipelines, gateway, CDN/SSR)_ |
| **Total engineer-months** | **~16–30** | **~22–40** |
| **Calendar, 1 engineer** | ~2–2.5 years | ~3+ years (not advisable) |
| **Calendar, 2–3 engineers (1 team)** | **~12–18 months** | ~18–24 months |
| **Calendar, 2 parallel teams (BE + FE)** | limited benefit (one codebase) | **~12–20 months** |
| **Go-live model** | near-big-bang; whole-product **feature freeze** for months | **incremental** — HTTP-layer strangler, page-by-page; freeze only the slice in flight |
| **Cutover risk** | High (single large switch, session flush) | Lower (each slice validated live before the next) |
| **Code discarded** | ~0% | frontend ~100% rebuilt |
| **Long-term maintainability** | modern framework, still an SSR monolith → re-platform again eventually | modern framework **and** architecture; future upgrades routine |
| **Hiring pool** | small (Grails niche) | large (Spring Boot + React) |

_Numbers are order-of-magnitude engineer-months, to be re-calibrated after the P0 audit and
the AnnotationStore spike (`06 §6`). Both paths carry the identical hard risks in `03`
(GORM re-cut, SHA-256→bcrypt, 2FA continuity, Solr 5→9, dead `routing` plugin)._

**Read:** Path A is the smaller total effort and keeps the most code, but concentrates risk
in one cutover and forces a long feature freeze. Path B costs more total effort (a full API
plus a frontend rebuild) yet lands in a similar calendar window when staffed as two teams,
removes the big-bang cutover and whole-product freeze (the dominant *programme* risks, `03`
G1), and fixes the architecture so this is the last re-platform. Decision gate for Path B:
prototype the strangler proxy + one React slice on a minimal Spring Boot backend before
committing (`07 §9`).
