# 06 — Migration Strategy, Phasing & Alternatives

## 1. Recommended approach: strangler re-platform, not in-place upgrade

Because there is no in-place path from Grails 2.5.5:

1. **Stand up a fresh Grails 7 application skeleton** (`grails create-app`, Gradle,
   `application.yml`, asset-pipeline) in a new module/repo layout.
2. **Migrate the domain artifact first** (`AnnotationStore` → GORM 9), then bring code across
   **plugin by plugin**, leaf plugins before `web-application`.
3. **Keep the Grails 2 app on a maintenance branch**, deployable, security-fixes-only, until
   the new app passes an agreed acceptance suite.
4. **Lock current behaviour with characterization tests** (HTTP-level, against the running
   old app) before porting each area — the unit test suite is non-functional until rewritten
   (128/131 files).
5. **Cut over once**, with a session-flush maintenance window and a user email (there is
   precedent — the 2026 security rollout).

### Target version decision

| Option | Pros | Cons | Recommendation |
|---|---|---|---|
| **Grails 6.2** (Groovy 3, `javax.*`, Java 17) | ~30% less blast radius (no Jakarta rename), still a supported runtime, all the hard 2→3 work done | Not the true "latest"; a second (smaller) migration to 7 later | **Interim target** — land here first |
| **Grails 7.0** (Groovy 4, `jakarta.*`, Spring Boot 3) | Actual latest; longest support runway; one migration not two | Every servlet/persistence-touching lib must have a Jakarta release; larger single step | **Final target** — go here directly only if every dependency in `04-*.md` is confirmed Jakarta-ready at kickoff |

Pragmatic path: **do the port targeting Grails 6.2, then take the 6→7 step as a follow-on**
once it is a routine bump. If the dependency audit at kickoff shows no Jakarta blockers, skip
6.2 and go straight to 7.

## 2. Phasing

| Phase | Scope | Exit criteria |
|---|---|---|
| **P0 — Foundations** | Dependency Jakarta-readiness audit; pick target version; new Gradle build skeleton; CI pipeline in-repo; characterization test harness against prod-like data; decide fate of DROP-candidate plugins (subversion, ast, remote, jms, jms-remote, annotation-source-ddmore) and of the service-shaped satellites | Target locked; green "hello world" Grails 7/6.2 app in CI; go/no-go on JMS + satellite re-homing |
| **P1 — Domain artifact** | `AnnotationStore` → GORM 9/Hibernate 6, `jakarta.persistence`, Groovy 4; read-only validation against a production DB snapshot; `database-migration` changelog baseline reconciliation | AnnotationStore `1.0.0` released; all entities load, query, and validate identically to old (SQL-diffed on hot paths) |
| **P2 — Indexer + native deps** | `biomodels-indexer` on new AnnotationStore; `CombineExt` / `libCombineArchive` / `ddi-ebeye-ws-dao` released against Java 17 (+ Jakarta if P0 chose 7); `FileService` to Spring Boot 3 | Indexer builds a byte-identical Solr index (against a new Solr 9 in parallel); native deps published |
| **P3 — Core + leaf plugins** | `core-api`, `configuration` (replace WebFlow wizard), `git`, `sbml`, `pharmml`, `mdl`, `combine-archive`, `common-format`, `bives`, `annotation-core`, `omicsdi`, `qc-info`, `simple-cms`, `simple-logging`; 8 Filters → Interceptors; 3 Camel routes → replacement; 13 Quartz jobs; port their tests | All non-web plugins compile & test green on the new stack; submission/VCS/format integration tests pass |
| **P4 — Web layer** | `jummp-plugin-web-application`: 170 GSPs, 18 controllers, all taglibs, `Team`/`Contributor*`; SiteMesh 3; asset-pipeline for 2,181 JS files; layouts (`main.gsp`, `newmain.gsp`); contextual-help build | Visual + functional parity on home, model show, search, submission wizard, admin, curation, team/contributor, auth UI |
| **P5 — Security hardening** | Password encoder migration (legacy `{sha256}` → bcrypt on next login); `spring-security-*` 6.x config port; REST `/api/**` chain rebuild + contract tests; 2FA / lockout / device-trust full regression; Spring Session 3; pen-test | Security review sign-off; all `/api/*` contract tests green; 2FA test matrix green |
| **P6 — Infra + cutover** | New `Dockerfile` (JDK 17/21 base, Tomcat 10.1 or executable jar), k8s manifests, `start.sh`, JVM flags; Solr 9 promotion + full reindex of 2,700+ models; DB driver bump; staging soak | Staging runs the full acceptance suite for an agreed soak period; rollback plan documented |
| **P7 — Production cutover** | Maintenance window, session flush, user comms, DNS/traffic switch, hypercare | Old app decommissioned after N days of clean operation |

## 3. Effort — order-of-magnitude bands

Deliberately coarse; calibrate after P0.

| Bucket | Rough size |
|---|---|
| AnnotationStore + indexer + native deps (P1–P2) | **1.5–3 engineer-months** |
| Build/config/CI skeleton, Filters→Interceptors, routes, jobs (P0, P3 infra) | **2–4 engineer-months** |
| Test-suite rewrite (128 files) + characterization tests | **2–4 engineer-months** (overlaps other phases) |
| Leaf plugins port (P3) | **3–5 engineer-months** |
| Web layer: 455 GSPs, SiteMesh 3, asset-pipeline, 14 taglibs, 18 controllers (P4) | **4–8 engineer-months** |
| Security workstream (P5) | **1.5–3 engineer-months** |
| Infra, Solr 9, cutover, soak (P6–P7) | **1.5–3 engineer-months** |
| **Total** | **~16–30 engineer-months** → **9–15 months calendar** with 2–3 engineers, assuming a partial feature freeze |

The dominant cost is the **web layer** (P4) and the **test rewrite**. The dominant *risk*
is the domain artifact (P1) and security (P5).

## 4. Hard prerequisites before committing

- [ ] Jakarta-readiness confirmed for every PORT dependency in `04-*.md` (or fall back to 6.2).
- [ ] Production `Config.groovy` external properties file obtained and understood.
- [ ] Decision on JMS remoting (keep → rewrite AST for Groovy 4; drop → delete 4 plugins).
- [ ] Decision on service-shaped satellites (Grails 7 vs Spring Boot 3 / Micronaut).
- [ ] Decision on which DB dialects remain in scope (drop unused ones from the test matrix).
- [ ] Solr 9 environment budgeted (parallel instance + reindex capacity).
- [ ] Feature-freeze scope agreed with stakeholders + a communicated timeline.
- [ ] A production DB snapshot available for the domain-artifact validation harness.

## 5. Lower-risk alternatives / de-scoping options

| Option | What it buys | What it costs |
|---|---|---|
| **Stop at Grails 6.2** (don't chase 7) | Supported runtime, Java 17, most CVEs closed, no Jakarta rename | Another migration due in a few years |
| **Drop the 6 dormant plugins** (subversion, ast, remote, jms, jms-remote, annotation-source-ddmore) | Removes ActiveMQ, AST-internals, SVN risk entirely; ~2 fewer plugins to port | Confirm nothing in ops depends on JMS export |
| **Retire the OmicsDI search backend** (keep Solr only) | Removes `ddi-ebeye-ws-dao` uplift + `omicsdi` plugin + Camel `ExportingOmicsDIRoute` | Lose the alternate search strategy (is it used in prod?) |
| **Consolidate to one DB engine** | Shrinks `DataSource`/dialect risk (C4) and the test matrix by 3× | Migration for any deployment on the dropped engines |
| **Move indexer / FileService / parameters off Grails** | Permanently removes them from future Grails upgrades | Rewrite cost now instead of later |
| **Decouple into Spring Boot API + React/TS frontend** (see `07-decoupled-architecture-option.md`) | Fixes the architecture not just the version; largest talent pools; HTTP-layer strangler removes the big-bang cutover and whole-product feature freeze | Larger total effort; complete REST API on the critical path; frontend rebuilt from zero; SSR/prerender needed for SEO; two pipelines + gateway |
| **Replace the in-house `simple-cms`** with a headless CMS or static content | Removes a plugin + TagLib + domain class + the Weceem-migration tail | Content migration effort |
| **Security-only holding pattern** (backport critical CVE fixes to Grails 2.5.5) | Buys time, near-zero risk | Not sustainable — dependencies are unpatchable, skills pool shrinking, feature velocity already constrained |

## 6. What to do first (regardless of the final decision)

1. **Run the P0 dependency Jakarta-readiness audit** — it decides 6.2 vs 7 and surfaces any
   true blockers cheaply.
2. **Write characterization tests** for auth, submission, model show, search reindex, VCS
   commit, and format detection **now**, against the Grails 2 app — they are the safety net
   for everything else and the knowledge to write them is eroding.
3. **Spike the AnnotationStore GORM 9 re-cut** on a branch against a DB snapshot — it is the
   critical path and the best early signal of how hard the persistence migration really is.
4. **Get a stakeholder decision on the feature freeze and timeline** — the programme risk
   (G1) is as likely to sink this as any technical risk.
