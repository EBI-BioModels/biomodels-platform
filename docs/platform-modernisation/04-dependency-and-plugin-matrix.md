# 04 — Dependency & Plugin Migration Matrix

Verdict key:
- **PORT** — a current version exists; expect config/API touch-ups.
- **REPLACE** — no viable current version; needs a different library/approach.
- **DROP** — candidate for removal (dead/disabled/unused).
- **RECUT** — in-house; must be re-released against the new stack (see `05-inhouse-projects.md`).

## Third-party Grails plugins

| Plugin (current) | Verdict | Target / replacement | Notes |
|---|---|---|---|
| `hibernate4:4.3.10` | PORT | GORM 9 + `grails-hibernate` (Hibernate 6.6) | Biggest persistence risk; Criteria API removed |
| `spring-security-core:2.0.0` | PORT | `grails-spring-security-core` 6.x | Property renames; `@Secured` + staticRules survive |
| `spring-security-acl:2.0.1` | PORT | `grails-spring-security-acl` 5.x/6.x | ACL schema unchanged; re-test model permission checks |
| `spring-security-ldap:2.0.1` | PORT | `grails-spring-security-ldap` 5.x/6.x | Currently near-disabled; confirm still needed |
| `spring-security-rest:1.5.3` | PORT | `grails-spring-security-rest` 3.x | Token storage/validation restructured; rebuild API chain |
| `spring-session:1.2` | PORT | Spring Session Data Redis 3.x | Serialization change → session flush at cutover |
| `quartz:1.0.2` | PORT | `grails-quartz` 3.x | Job class API changed; re-test all 13 jobs |
| `mail:1.0.7` | PORT | `grails-mail` 4.x | Minor; verify the shared HTML email shell (recent JBM-729 work) |
| `cache:1.1.8` | PORT | `grails-cache` 5.x | Backing store change |
| `cache-ehcache:1.0.5` | REPLACE | Caffeine (Spring Boot 3 default) or Ehcache 3 / JCache | Ehcache 2 is dead |
| `database-migration:1.4.1` | PORT | `grails-database-migration` 4.x (Liquibase 4) | Changelog baseline reconciliation (risk C3) |
| `cors:1.3.0` | REPLACE | Grails built-in CORS (`grails.cors.*` in `application.yml`) | Plugin obsolete since Grails 3.2 |
| `routing:1.3.2` | REPLACE | Camel 4 on Spring Boot 3, or Spring events + Quartz | **Plugin abandoned** — risk B1 |
| `webflow:2.1.0` | REPLACE | Linear multi-step controller (drop WebFlow) | `grails-webflow` exists but weak — risk B2 |
| `simple-captcha:1.0.0` | REPLACE | reCAPTCHA / hCaptcha / maintained captcha lib | No current version |
| `perf4j:0.2.1` | REPLACE | Micrometer + Actuator | Dead since 2011, Log4j-1 |
| `webxml:1.4.1` | REPLACE | Spring Boot servlet/filter registration beans | No `web.xml` in Boot |
| `locale-variant:0.1` | REPLACE | Grails i18n + custom `LocaleResolver`, or merge variant GSPs | Niche plugin, no current version |
| `console:1.5.8` | PORT | `grails-console` 2.x | Dev-only; low risk |
| `scaffold-core:1.3.2` | PORT/DROP | Built-in scaffolding | Check if actually used at runtime |
| `codenarc:1.2` | PORT | CodeNarc 3.x Gradle plugin | Rule set changes → warning noise |
| `tomcat:7.0.55.3` | REPLACE | Spring Boot embedded Tomcat 10.1 | Container model changes entirely |
| `rest-client-builder:2.1.1` | PORT/REPLACE | `grails-rest-client-builder` 3.x or Spring `RestClient`/`WebClient` | `BootStrap` strips its GSON converter — re-check |

## In-tree Jummp plugins (all need Grails 7 plugin-layout restructuring)

| Plugin | Verdict | Special concerns |
|---|---|---|
| `jummp-plugin-core-api` | PORT | Domain adapters depend on AnnotationStore (RECUT first); raw jedis usage; REST token services |
| `jummp-plugin-configuration` | PORT | Contains the **WebFlow** setup wizard (REPLACE the flow); LDAP command |
| `jummp-plugin-web-application` | PORT | 170 GSPs, 18 controllers, all taglibs, ex-security `Team`/`Contributor*`; migrate **last** |
| `jummp-plugin-biomodels-dom` | PORT | Largest; 6 local domain classes → GORM 9; home-page Redis caching; competitions |
| `jummp-plugin-git` | PORT | JGit bump (current JGit needs Java 11+); pure-Java, no shell-out — relatively contained |
| `jummp-plugin-sbml` | PORT | JSBML version bump; TagLib; pulls `webflow` transitively today |
| `jummp-plugin-pharmml` | PORT | PharmML libs (in-house-adjacent); 15 GSPs; TagLib |
| `jummp-plugin-mdl` | PORT | Depends on pharmml; TagLib |
| `jummp-plugin-combine-archive` | PORT | Depends on in-house `CombineExt` + `libCombineArchive` (RECUT) |
| `jummp-plugin-common-format` | PORT | Pure format sniffing; low risk |
| `jummp-plugin-bives` | PORT | BiVeS diffing lib bump; no tests |
| `jummp-plugin-annotation-core` | PORT | RDF/SBML writers; Jena bump; **no tests** — add characterization tests first |
| `jummp-plugin-omicsdi` | PORT | `ddi-ebe-ws-dao:1.3-SNAPSHOT` (in-house/EBI, SNAPSHOT — pin it); embeds OmicsDI schema |
| `jummp-plugin-qc-info` | PORT | Small |
| `jummp-plugin-simple-cms` | PORT | CMS with 1 domain class + TagLib; check `migration-weceem-scms` sibling status |
| `jummp-plugin-simple-logging` | PORT/REPLACE | Log4j-1 / logback-0.9 era; move to Logback 1.4 / SLF4J 2 (Spring Boot 3 default) |
| `jummp-plugin-subversion` | DROP | Already disabled; delete unless SVN is coming back |
| `jummp-plugin-ast` | DROP (preferred) | Groovy-4 compiler-internal rewrite if JMS kept; else delete |
| `jummp-plugin-remote` | DROP (preferred) | Only used by JMS remoting |
| `jummp-plugin-jms` | DROP (preferred) | ActiveMQ 5.5 (2011) — would need Artemis; env-gated, no tests |
| `jummp-plugin-jms-remote` | DROP | Commented out already |
| `jummp-plugin-annotation-source-ddmore` | DROP | Commented out already |

**If the 6 DROP-preferred plugins go**, the active plugin count falls from 16 to ~14 and a
whole class of risk (F6, ActiveMQ, AST internals) disappears. Recommend confirming with the
team early.

## Library dependencies

| Library (current) | Verdict | Target | Notes |
|---|---|---|---|
| `solr-solrj:5.4.1` | PORT | SolrJ 9.x | Forces Solr **server** 9 upgrade + reindex (risk F1) |
| `jena-tdb/core/arq:3.17.0` | PORT | Jena 5.x (`jena-tdb2`) | Java 17; TDB2 migration (risk F3) |
| `spring-data-redis:1.8.10` | PORT | 3.x | With jedis 5 or Lettuce (risk F4) |
| `redis.clients:jedis:2.9.0` | PORT | 5.x (breaking) or switch to Lettuce | Rewrite in-house Redis clients |
| `tika-core:1.23` | PORT | Tika 2.x/3.x | API moves; used for format/mime detection |
| `jackson-*:2.9.0` | PORT | 2.17+ | 2.9 has CVEs; Boot 3 manages the version |
| `xstream:1.4.7` | PORT | 1.4.21 | Multiple CVEs in 1.4.7; needed by JSBML transitively |
| `woodstox-core-lgpl:4.4.1` | PORT | `woodstox-core` 6.x | LGPL artifact renamed |
| `logback-core:0.9.29` | REPLACE | Logback 1.4.x + SLF4J 2 | Ancient; Boot 3 default |
| Log4j-1 appenders (in `Config.groovy`) | REPLACE | Logback / Log4j2 config | Log4j 1.x is EOL + CVEs |
| `postgresql:9.1-901.jdbc4` | REPLACE | `org.postgresql:postgresql` 42.7.x | Decade-old driver |
| `mysql-connector-java:8.0.11` | PORT | `com.mysql:mysql-connector-j` 8.4.x | Coordinate change |
| `mariadb-java-client:2.7.12` | PORT | 3.x | |
| `commons-jexl:1.1` | PORT | JEXL 3.x (big API change) or remove | Check usage |
| `camel-exec:2.13.0` | PORT/REPLACE | Camel 4 or shell-out via `ProcessBuilder` | Tied to routing plugin decision |
| `rome:1.11.1` | PORT | 2.1.x | Atom/RSS feed generation |
| `icu4j:4.8.1` | PORT | 74+ | Very old |
| `tagsoup:1.2` | PORT | keep / jsoup | HTML cleanup |
| JSBML (via sbml plugin) | PORT | latest JSBML | In-house-adjacent (SBML Team); coordinate |
| `ddi-ebe-ws-dao:1.3-SNAPSHOT` | RECUT/PIN | EBI-owned; pin a release, not SNAPSHOT | OmicsDI DAO |
| `libCombineArchive:0.3.2-SNAPSHOT` | RECUT | in-house (uni-rostock/mbine) | SNAPSHOT → release |
| `CombineExt:1.3.1` | RECUT | in-house | |
| `annotationstore:0.3.7-SNAPSHOT` | RECUT | in-house — **phase 0** | risk C1 |
