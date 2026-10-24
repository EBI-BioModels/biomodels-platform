# 05 — In-House Projects That Must Migrate in Lockstep

The team maintains several of the "third-party" dependencies. That is an advantage (we
control the release schedule) but it also means the migration scope is **wider than this
repo** — the coupled projects have to move in a specific order.

## The coupled set

| Project | Repo / coords | Current stack | Coupling to the main app | Verdict |
|---|---|---|---|---|
| **AnnotationStore** | `../annotationstore`, `net.biomodels.jummp:annotationstore:0.3.7-SNAPSHOT` (GitHub Packages; lowercase artifactId since JBM-717) | Maven, Groovy **2.4.4**, `grails-spring:2.5.4`, `grails-datastore-gorm-hibernate4:3.1.5.RELEASE`, `maven.compiler {source,target} = 1.7` | **Supplies the entire core domain model** (`Model`, `Revision`, `Publication`, `RepositoryFile`, `ModelFormat`, `PublicationLinkProvider`, `User`, `Role`, `UserRole`, `Person`, `AuthToken`). Compiled into both the app and the indexer. | **RECUT — phase 0, blocking** |
| **biomodels-indexer** (`JummpSolrIndexer`) | `../biomodels-indexer`, Maven | Spring Boot, Groovy 2.4 `groovy-all`, `grails-datastore-gorm-hibernate4`, `grails-datastore-core`, `grails-spring`, `gorm-hibernate4-spring-boot`, `solr-solrj` (Solr 5) | Reads AnnotationStore entities directly via GORM to build the Solr index; same DB. Shares the `annotationstore` artifact version (pinned separately in the indexer's `pom.xml`, resolved from the same GitHub Packages repository). | **RECUT — phase 1** (right after AnnotationStore) |
| **CombineExt** | `../dependencies/CombineExt`, `de.uni-rostock.sbi:CombineExt:1.3.1` (repo says `1.3.2-SNAPSHOT`) | Maven, Java lib, jdom/jaxb-era deps | Used by `jummp-plugin-combine-archive` for OMEX handling | **RECUT — pin a release, Jakarta-check JAXB** |
| **libCombineArchive / CombineArchive** | `../dependencies/CombineArchive`, `org.mbine.co:libCombineArchive:0.3.2-SNAPSHOT` | Maven, `maven.compiler.source = 8` | Used by `jummp-plugin-combine-archive` | **RECUT — release the SNAPSHOT; JAXB/Java-17 check** |
| **ddi-ebeye-ws-dao** | `../dependencies/ddi-ebeye-ws-dao`, `uk.ac.ebi.ddi:ddi-ebe-ws-dao:1.3-SNAPSHOT` | Maven, **Spring 4.2.2**, Jackson 1.6.x era | Used by `jummp-plugin-omicsdi` for the OmicsDI search backend | **RECUT — Spring 6 / Jackson 2.17 uplift, or replace with a thin REST client** |
| **FileService** (`omex-generator` / `fileservice` / `fileservice2`) | `../biomodels-services/fileservice*`, Spring Boot, `<java.version>1.8`  | Spring Boot 2 / Java 8 standalone service | Not a compile dependency of the app — it's a **separate deployable** the app calls over HTTP (OMEX generation / file serving). | **PORT independently** — Spring Boot 2→3, Java 8→17; not on the critical path but must stay API-compatible |
| **JSBML** | SBML Team (`../sbmlteam`, `../test-jsbml-features`) | — | `jummp-plugin-sbml` | **PORT** — bump to current JSBML; coordinate with SBML Team release |
| **biomodels-parameters** | `../biomodels-parameters`, Maven, Java 8 | parameter extraction | feeds parameter search in `biomodels-dom` | **PORT** — Java 8→17 |
| **biomodels-converters** | `../biomodels-converters` | model format converters | linked from the UI (converters menu, JBM-721) | **PORT / independent** |
| jummp-contextual-help | `../jummp-contextual-help(-build)` | contextual help content build | feeds `ContextHelpTagLib` / `main.gsp` help panel | **PORT** — asset-pipeline alignment |

## Ordering constraint (hard dependencies first)

```
Phase 0   AnnotationStore                → GORM 9 / Hibernate 6 / Groovy 4 / jakarta.persistence, new release X.Y.Z
             │  (validate against a production DB snapshot, read-only, before anything depends on it)
             ▼
Phase 1   biomodels-indexer              → same GORM/Groovy/Java baseline, depends on AnnotationStore:X.Y.Z
          CombineExt, libCombineArchive  → pin releases, Java 17 / JAXB-Jakarta
          ddi-ebeye-ws-dao               → Spring 6 uplift (or replace)
          FileService                    → Spring Boot 3 / Java 17 (parallel, API-compatible)
             ▼
Phase 2   jummp-biomodels core + leaf plugins (core-api, configuration, git, formats, …)
             ▼
Phase 3   jummp-plugin-web-application (views, controllers, taglibs, auth UI)
             ▼
Phase 4   Solr server 9 + reindex, cutover, session flush
```

## Version-bump choreography

Any AnnotationStore schema/API change requires a coordinated bump in **two** places today:
`jummpDependencyVersions.properties` (one edit that feeds `BuildConfig.groovy`,
`jummp-plugin-configuration` and `jummp-plugin-core-api`) and the `biomodels-indexer`
`pom.xml`. During the migration the app's `build.gradle` becomes a third. Recommend:

- Cut AnnotationStore as `1.0.0` (clean break from the `0.3.x` line) so it is obvious which
  releases are Jakarta-era.
- Extend the single-source-of-truth approach already used for the app
  (`jummpDependencyVersions.properties`) to a BOM / `gradle.properties` version catalog shared
  with the indexer, so the bump is one line.
- Keep the `0.3.x` line alive on a branch until the indexer and app are both on `1.0.0`.

## Opportunity: which satellites should leave Grails/GORM entirely?

`biomodels-indexer`, `FileService`, `ddi-ebeye-ws-dao`, `biomodels-parameters` are all
**service-shaped, not web-app-shaped**. They currently borrow GORM/Grails purely for entity
mapping or Spring plumbing. Moving them to **GORM-standalone on Spring Boot 3** (indexer) or
**plain Spring Boot 3 / Micronaut** (the rest) removes them from the Grails upgrade treadmill
permanently. Worth an explicit decision at kickoff rather than reflexively porting them to
Grails 7.
