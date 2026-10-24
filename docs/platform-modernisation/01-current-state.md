# 01 — Current State (the thing being moved)

Everything below was read from the repository and sibling checkouts on 2026-09-06.

## 1. Framework & runtime

| Component | Current version | Status upstream |
|---|---|---|
| Grails | **2.5.5** (`application.properties: app.grails.version`) | EOL since ~2017; no security patches |
| Groovy | **2.4.21** (`groovy-backports-compat23:2.4.21`; AnnotationStore pins 2.4.4) | EOL; Parrot parser era not yet reached |
| JVM | **Java 8** (Corretto 1.8.0_504; `grails.project.source.level = 1.8`, `target.level = 1.8`) | Java 8 in extended support only |
| Servlet API | **2.5 / 3.0** (`app.servlet.version=2.5`, `grails.servlet.version = "3.0"`) | Pre-Jakarta |
| Persistence | **GORM for Hibernate 4** (`hibernate4:4.3.10` plugin; AnnotationStore uses `grails-datastore-gorm-hibernate4:3.1.5.RELEASE`) | GORM 4-era; current is GORM 9 |
| Spring | **4.x** (transitive via Grails 2.5.5) | EOL |
| Build | Grails 2 build + **Maven resolver**, `BuildConfig.groovy` (Ivy-style DSL) | Replaced by Gradle in Grails 3 |
| View layer | **GSP + SiteMesh 2** | SiteMesh 3 in Grails 5+ |
| Assets | `resources` plugin family (`<r:...>` tags seen in views) | Removed; asset-pipeline is the replacement |
| App server (prod) | **Tomcat 7** (`tomcat:7-jdk8` base image; `tomcat:7.0.55.3` build plugin) | Tomcat 7 EOL 2021; Jakarta needs Tomcat 10.1+ |
| Packaging | **WAR** deployed to external Tomcat | Grails 3+ favours executable JAR (Spring Boot) |
| Container base | `openjdk:8-jdk-buster` / `tomcat:7-jdk8-openjdk-buster` | Both EOL Debian + EOL JDK image |

## 2. Code volume

| Artefact | Count |
|---|---|
| Groovy source files | 999 |
| Java source files | 16 |
| GSP views | **455** (170 in web-application, 65 in biomodels-dom, 49 in configuration, 15 in pharmml, …) |
| JS files (excl. node_modules) | 2,181 |
| Controllers (`*Controller.groovy`) | 61 |
| Services (`*Service.groovy`) | 94 |
| Tag libraries | 14 |
| Filter classes (`*Filters.groovy`) | 8 in core app (`VerifyOtpFilters`, `ModelFilters`, `BrandingFilters`, `AdminHeaderFilters`, `ParameterFilters`, `PreProcessFilters`, `RequestLoggingFilters`, `SetupFilters`) |
| Camel route definitions (`grails-app/routes/`) | 3 (`ExportingOmicsDIRoute`, `IndexingRoute`, `NotificationRoute`) |
| Quartz jobs (`grails-app/jobs/`) | 13 |
| Database-migration changelogs | 110 (2014 → present) |
| `spring/resources.groovy` bean files | 5 (core + git, pharmml, jms, subversion plugins) |
| Total Groovy+Java LOC | ~114,785 |

## 3. In-tree Grails plugins (`jummp-plugins/`)

21 plugin directories; **16 active** in a default build (`ast`/`remote`/`jms` are env-gated;
`jms-remote`/`annotation-source-ddmore`/`subversion` are commented out). Each declares
`grailsVersion = "<old> > *"` and carries a Grails 2 plugin descriptor
(`*GrailsPlugin.groovy`), its own `grails-app/conf/`, and its own `grailsw`.

| Plugin | GSPs | Notes for migration |
|---|---:|---|
| `jummp-plugin-core-api` | 4 | Foundation: DTOs, interfaces, **domain adapters**, Redis utils, REST token services |
| `jummp-plugin-configuration` | 49 | First-run **WebFlow** wizard (`SetupController` — the only flow controller), optional LDAP |
| `jummp-plugin-web-application` | **170** | Main MVC layer; also holds `Team`/`Contributor*` (ex-security plugin) |
| `jummp-plugin-biomodels-dom` | 65 | Largest domain plugin: home page, classification, competitions, parameter search, 6 local domain classes |
| `jummp-plugin-git` | 3 | JGit-backed VCS |
| `jummp-plugin-sbml` | 8 | JSBML; has a TagLib |
| `jummp-plugin-pharmml` | 15 | PharmML; TagLib |
| `jummp-plugin-mdl` | 6 | MDL; TagLib; depends on pharmml |
| `jummp-plugin-combine-archive` | 4 | OMEX via `libCombineArchive` + `CombineExt` (in-house) |
| `jummp-plugin-common-format` | 3 | Non-SBML format sniffing |
| `jummp-plugin-bives` | 3 | Model diffing |
| `jummp-plugin-annotation-core` | 1 | RDF/SBML annotation writers; **no tests** |
| `jummp-plugin-omicsdi` | 3 | Alt search backend; embeds an OmicsDI schema |
| `jummp-plugin-qc-info` | 2 | QC metadata |
| `jummp-plugin-simple-cms` | 12 | In-house CMS; 1 domain class; **has a TagLib** |
| `jummp-plugin-simple-logging` | 3 | Log config only |
| `jummp-plugin-subversion` | 1 | **Disabled** — legacy SVN |
| `jummp-plugin-ast` | 1 | Groovy **AST transformations** for JMS remoting; env-gated |
| `jummp-plugin-remote` / `jms` / `jms-remote` | 1 each | JMS/ActiveMQ remoting; disabled/env-gated |
| `jummp-plugin-annotation-source-ddmore` | 3 | DDMoRe SPARQL; commented out |

## 4. Third-party plugin dependencies (`BuildConfig.groovy` `plugins { }`)

```
tomcat:7.0.55.3          codenarc:1.2            rest-client-builder:2.1.1
cache:1.1.8              cache-ehcache:1.0.5     webxml:1.4.1
perf4j:0.2.1             routing:1.3.2           mail:1.0.7
simple-captcha:1.0.0     quartz:1.0.2           scaffold-core:1.3.2
spring-security-acl:2.0.1   spring-security-core:2.0.0   spring-security-ldap:2.0.1
locale-variant:0.1       webflow:2.1.0           spring-session:1.2
spring-security-rest:1.5.3  cors:1.3.0           database-migration:1.4.1
hibernate4:4.3.10        console:1.5.8
```

## 5. Notable library dependencies (`BuildConfig.groovy` `dependencies { }`)

- `org.apache.solr:solr-solrj:5.4.1` (client tied to Solr 5.4.1 server)
- `mysql-connector-java:8.0.11`, `mariadb-java-client:2.7.12`, `postgresql:9.1-901.jdbc4` (very old PG driver)
- `org.apache.jena:jena-tdb/jena-core/jena-arq:3.17.0` (RDF triple store)
- `org.apache.tika:tika-core:1.23`
- `com.fasterxml.jackson.*:2.9.0`
- `spring-data-redis:1.8.10.RELEASE` + `redis.clients:jedis:2.9.0`
- `com.thoughtworks.xstream:xstream:1.4.7` (known CVE history)
- `ch.qos.logback:logback-core:0.9.29` (ancient), Log4j 1.x appenders referenced in `Config.groovy`
- `org.codehaus.woodstox:woodstox-core-lgpl:4.4.1`
- **In-house:** `net.biomodels.jummp:annotationstore:0.3.7-SNAPSHOT`, `de.uni-rostock.sbi:CombineExt:1.3.1`,
  `org.mbine.co:libCombineArchive:0.3.2-SNAPSHOT`, `uk.ac.ebi.ddi:ddi-ebe-ws-dao:1.3-SNAPSHOT`
- JSBML / PharmML / BiVeS / MDL parsers (transitively, via the format plugins)

## 6. Security & session configuration (`Config.groovy`)

- `grails.plugin.springsecurity.password.algorithm = 'SHA-256'`, `password.hash.iterations = 1`
- `securityConfigType = "Annotation"` (`@Secured` on controllers + `staticRules`)
- REST login at `/api/login`, GORM-backed tokens (`AuthToken` domain class in AnnotationStore),
  `X-Auth-Token` header, custom stateless `filterChain.chainMap` for `/api/**`
- `userLookup.userDomainClassName = net.biomodels.jummp.plugins.security.User` (in AnnotationStore)
- Optional LDAP (`ldaps://ldap.ebi.ac.uk`) — currently effectively disabled
- `spring-session:1.2` with Redis (`spring-data-redis` excluded from the plugin, provided at app level)
- 2FA: `TwoFactorAuth` domain class, `security.AuthController`, `VerifyOtpFilters`, Redis-backed
  `LoginAttemptCacheService`, jobs `UnlockAccountJob` / `AuthenticationOutdaterJob` / `RegistrationCheckerJob`

## 7. Build / deploy pipeline

- No `.github/`, no `Jenkinsfile`, no `.gitlab-ci.yml` in this repo — CI lives elsewhere or is manual.
- `Dockerfile` is multi-stage: `base` (installs Grails 2.5.5 from GitHub release zip) →
  `deploy` (`tomcat:7-jdk8`, explodes `target/biomodels.war`) → `prod` / `develop` / `debug`.
- JVM opts baked in: `-noverify` (removed in Java 13+), `-XX:MaxPermSize` (removed in Java 8+,
  ignored), CMS GC (`-XX:+UseConcMarkSweepGC` — removed in Java 14+).
- `scripts/` holds ~18 standalone Grails scripts (batch import, cache rebuild, Uhlen updater,
  Weceem export) invoked via the Grails 2 script runner — that runner does not exist in Grails 3+.
