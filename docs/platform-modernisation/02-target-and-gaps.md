# 02 — Target Stack and Structural Gaps

## 1. What "the latest full-stack framework" means

| Grails | Released | Groovy | Spring Boot | Spring | Java | GORM / Hibernate | Namespace |
|---|---|---|---|---|---|---|---|
| 2.5.5 (**now**) | 2016 | 2.4 | — (bespoke runtime) | 4.x | 7/8 | GORM 4 / Hibernate 4 | `javax.*` |
| 3.3.x | 2017 | 2.4 | 1.5 | 4.3 | 8 | GORM 6 / Hibernate 5 | `javax.*` |
| 4.1.x | 2020 | 2.5 | 2.1 | 5.1 | 8/11 | GORM 7 / Hibernate 5 | `javax.*` |
| 5.3.x | 2022 | 3.0 | 2.6 | 5.3 | 8/11 | GORM 7 / Hibernate 5 | `javax.*` |
| 6.2.x | 2023 | 3.0 | 2.7 | 5.3 | 11/17 | GORM 8 / Hibernate 5.6 | `javax.*` |
| **7.0.x** (target) | 2025 | **4.0** | **3.5** | **6.2** | **17 min, 21 rec.** | **GORM 9 / Hibernate 6.6** | **`jakarta.*`** |

Going to the true latest (7.0) crosses **two** epochs:

- **2.x → 3.x**: build system, runtime model, config format, plugin model, request pipeline,
  view/asset stack all replaced. This is the expensive one and it is unavoidable.
- **6.x → 7.x**: the Jakarta EE migration — `javax.servlet` / `javax.persistence` /
  `javax.validation` / `javax.annotation` → `jakarta.*` everywhere, Spring 6, Hibernate 6,
  Tomcat 10.1+. Every library that touches the servlet or persistence API must have a
  Jakarta-era release.

A defensible **intermediate target is Grails 6.2** (still `javax.*`, Groovy 3, Java 11/17):
smaller blast radius, buys a supported runtime, and leaves the Jakarta jump as a later,
better-understood step. See `06-migration-strategy.md`.

## 2. There is no in-place upgrade from 2.5.5

Grails provides an upgrade guide only from 3.x forward. From 2.x the official position (and
universal community practice) is: **create a fresh Grails app with the current tooling and
move source into it**, package by package, because —

| Grails 2.5.5 | Grails 6/7 | Migration action |
|---|---|---|
| `BuildConfig.groovy` (Ivy/Maven DSL), `grailsw` wrapper | `build.gradle` + Gradle wrapper, `gradle.properties` | Rewrite build from scratch; re-express every dependency & exclusion |
| `grails-app/conf/Config.groovy` (Groovy DSL, 1,095 lines) | `application.yml` + `application.groovy` + `runtime.groovy` | Port ~1,100 lines of config, much of it environment-branched |
| `grails-app/conf/DataSource.groovy` | merged into `application.yml` | Re-express the 3-dialect switch |
| `grails-app/conf/*Filters.groovy` (8 classes) | **Interceptors** (`grails-app/controllers/**/*Interceptor.groovy`) | Rewrite all 8; `before`/`after`/`afterView` semantics and ordering differ; `filtersInterceptor` chain → `@Order` / `match()` |
| `grails-app/conf/BootStrap.groovy` | still supported | Minor API touch-ups |
| `grails-app/conf/UrlMappings.groovy` | still supported | Mostly survives; dynamic regex assembly needs review |
| `grails-app/conf/QuartzConfig.groovy` + `quartz:1.0.2` | `grails-quartz` 2.x / 3.x | Job class API changed; re-test all 13 jobs |
| `grails-app/routes/*` + `routing:1.3.2` (Apache Camel DSL) | **plugin abandoned** — no Grails 3+ release | Re-implement 3 routes as Spring-managed Camel contexts or Spring Integration, or fold into services + Quartz |
| WebFlow (`webflow:2.1.0`, `SetupController`) | `grails-webflow` exists but lags; flow DSL changes | Rewrite the first-run setup wizard (49 GSPs in `configuration` plugin), or replace with a plain multi-step controller |
| SiteMesh 2 layouts | SiteMesh 3 | Layout resolution, `<g:layoutBody/>`, decorator mapping all change; re-verify all shared layouts (`biomodels/main.gsp`, `newmain.gsp`) and 455 views |
| `resources` plugin (`<r:require>`, `<r:layoutResources>`) | **asset-pipeline** | Re-declare every JS/CSS bundle; 2,181 JS files + module config to reorganise under `grails-app/assets/` |
| In-place source plugins (`grails.plugin.location.*`) | Gradle multi-project or published binary plugins | Restructure all 21 plugins: new descriptor (`*GrailsPlugin.groovy` → plugin class + `build.gradle`), `plugin.xml` gone, `_Install/_Upgrade/_Uninstall.groovy` scripts gone |
| `grails.test.mixin.*`, `GrailsUnitTestCase` (128 of 131 test files) | `grails-testing-support` (`@TestFor` → `Specification` + traits like `ControllerUnitTest`) | Rewrite the entire test suite |
| Grails 2 script runner (`scripts/*.groovy`, 18 files) | Gradle tasks / `grails` command / standalone `@GrailsCompileStatic` scripts | Re-home every operational script |
| `hibernate4` plugin | `grails-hibernate5` (G6) / GORM 9 + Hibernate 6 (G7) | Mapping/query re-validation (see gap #4 below) |

## 3. Groovy 2.4 → 4.0 gaps

- **Parrot parser** (default from Groovy 3): stricter grammar; some 2.4-valid syntax breaks.
- `groovy-all` pom removed in Groovy 4 — must depend on specific modules.
- `@CompileStatic` type-checking is stricter; latent type errors in 999 files surface at
  compile time.
- Groovy 4 moved to the `org.apache.groovy` group id and `groovy.*` some packages moved.
- Third-party AST-transformation code (`jummp-plugin-ast`, 5 classes) uses internal Groovy
  compiler APIs (`org.codehaus.groovy.*`) that were repackaged/changed — needs a rewrite if
  JMS remoting is kept.
- `groovy-backports-compat23` (currently pinned) becomes meaningless and must be removed.

## 4. GORM 4 → 9 / Hibernate 4 → 6 gaps

- **`javax.persistence` → `jakarta.persistence`** (G7) — affects `@Entity` classes in
  `AnnotationStore` and any explicit annotations.
- Hibernate 6 rewrote the query engine: HQL is stricter, some implicit joins and
  `executeQuery` string forms (19 usages found) change behaviour; `createCriteria` /
  `withCriteria` (14 usages) — the legacy Criteria API is **removed in Hibernate 6**, GORM's
  `DetachedCriteria` is the replacement but not 100% equivalent.
- Dirty-checking, cascade defaults, `null`/blank handling, and `flush` timing differ — subtle
  data-integrity risks in the submission/versioning flow.
- Old PostgreSQL driver (`9.1-901.jdbc4`) and `MySQL5InnoDBDialect` must be replaced;
  dialect class names moved packages in Hibernate 6.
- The `database-migration` plugin (`1.4.1`) → the modern Grails `database-migration` plugin
  is Liquibase 4-based; the 110 existing changelogs need format validation and a fresh
  baseline/checksum reconciliation against production schema.

## 5. Jakarta EE gap (only if targeting Grails 7)

Every one of these must have a Jakarta-compatible release, or a replacement:
`spring-security-*` plugins, `spring-security-rest`, `spring-session`, `cors`,
`mail`, `quartz`, `cache*`, `simple-captcha`, `webxml`, Tika, SolrJ, Jena, Jackson,
the MySQL/MariaDB/Postgres drivers, JGit, `redis`/`jedis`, Woodstox, XStream, JSBML,
libCombineArchive, CombineExt, ddi-ebe-ws-dao. Servlet-touching in-house code
(`RequestLoggingFilters`, REST controllers, healthcheck) needs the namespace change too.

## 6. Infrastructure gaps

- **Container**: Tomcat 7 → Tomcat 10.1 (Jakarta) or drop the external container and ship the
  Spring Boot executable WAR/JAR. Base images `openjdk:8` / `tomcat:7` are EOL and unpatched.
- **JVM flags**: `-noverify`, `-XX:MaxPermSize`, CMS GC all gone in Java 17 — the
  `Dockerfile` and any k8s manifests / `start.sh` need rewriting (G1/ZGC, `-XX:MaxRAMPercentage`).
- **Solr 5.4.1**: SolrJ 8/9 clients target Solr 8/9; a SolrJ bump forces a Solr **server**
  upgrade (schema.xml → managed schema, `solrconfig.xml` changes, reindex of 2,700+ models).
- **Redis clients**: `jedis` 2.9 → 5.x (API overhaul) or switch to Lettuce (Spring Boot 3
  default); `spring-data-redis` 1.8 → 3.x.
- **Java driver / TLS**: newer JVM enforces stronger TLS defaults — LDAP (`ldaps://`) and
  any outbound HTTPS (PubMed, DOI, identifiers.org, OmicsDI) need re-testing.
