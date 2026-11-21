# 07 — Decoupled Architecture Option (Spring Boot backend + React/TS frontend)

This documents the alternative to a like-for-like Grails 7 re-platform: **split BioModels
into two repositories** — a headless **Spring Boot** JSON API backend and a standalone
**TypeScript + React** frontend — so each side can pick its own stack and release cadence.

It is written as a *comparison* against the Grails 7 full-stack path in `06-migration-strategy.md`.

---

## 1. What actually changes on each side

### Backend (Grails full-stack → Spring Boot 3, headless)

| Concern | Grails 2.5.5 today | Grails 7 path | Spring Boot headless path |
|---|---|---|---|
| Framework | Grails 2 (Spring MVC under the hood) | Grails 7 (Spring Boot 3 + conventions) | Spring Boot 3 directly, no Grails |
| Controllers | 61 Grails controllers (render views + JSON) | ported Grails controllers | `@RestController` classes, JSON only |
| View layer | 455 GSP + SiteMesh 2 + 14 taglibs | GSP + SiteMesh 3 + asset-pipeline | **deleted** — no server-side views at all |
| Request pipeline | 8 `*Filters.groovy` | 8 Grails interceptors | Spring `OncePerRequestFilter` / `HandlerInterceptor` / Security filter chain |
| Persistence | GORM/Hibernate 4 (domain in AnnotationStore) | GORM 9/Hibernate 6 | GORM-standalone on Spring Boot **or** migrate to Spring Data JPA / jOOQ |
| Plugin model | 21 in-tree Grails plugins | 21 restructured Grails 7 plugins | Gradle/Maven multi-module (Spring `@Configuration`), optionally Spring Modulith |
| Format libs (sbml/pharmml/mdl/bives/combine/common-format) | Grails plugins wrapping Java/Groovy libs | Grails plugins | plain library modules (they barely used Grails) |
| Setup wizard | WebFlow (`SetupController`) | rewrite (WebFlow is weak in G7) | plain multi-step REST + config endpoints |
| Jobs | Quartz plugin, 13 jobs | `grails-quartz` | Spring `@Scheduled` / Quartz starter |
| Camel routes (3) | `routing` plugin (dead) | bespoke replacement | Camel 4 on Spring Boot, or Spring events |
| Search / cache / VCS | SolrJ 5, jedis 2.9, JGit | bumped | bumped (same libraries, no Grails coupling) |
| Auth | spring-security-core 2.0 + rest plugin, session + token, 2FA filter | spring-security 6.x plugins | Spring Security 6 directly + one token model |
| Packaging | WAR on Tomcat 7 | Boot fat JAR/WAR | Boot fat JAR |
| Config | `Config.groovy` (1,095 lines) | `application.yml` + `application.groovy` | `application.yml` + `@ConfigurationProperties` |
| API docs | none | none | OpenAPI via springdoc; generated TS client |

**Net:** roughly the *same* service/domain/integration porting work as the Grails 7 backend
(94 services, Solr, Redis, Git, Quartz, format libs, AnnotationStore re-cut all still
required), **minus** GSP/SiteMesh/asset-pipeline/taglib/WebFlow/Grails-plugin-restructuring,
**plus** more explicit Spring wiring and an OpenAPI contract to maintain. The half of
`jummp-plugin-web-application` that is views + taglibs simply disappears.

### Frontend (server-rendered GSP → React SPA)

| Concern | Today | React/TS path |
|---|---|---|
| Rendering | GSP server-side, jQuery sprinkles (~2,181 JS files) | React components, client-side; SSR/prerender for public pages (see §5) |
| Routing | Grails `UrlMappings` + controller actions | React Router (or Next.js file routing) |
| Build | `resources` plugin | Vite / Next.js, npm/pnpm, its own CI |
| State/data | request-scoped model in GSP | React Query / RTK Query against the API |
| i18n | GSP `message` tags + `locale-variant` plugin | i18next / FormatJS |
| Auth UI | GSP login/2FA/enrolment pages | React forms hitting auth API |
| Deploy | inside the WAR | static bundle on CDN, or Node server if SSR |
| Reuse from current UI | — | **~0%** — every one of the 455 GSPs + taglibs is re-authored as components |

**Net:** the frontend is a **greenfield rewrite**. More raw work than porting GSPs to
SiteMesh 3, but parallelisable with a dedicated frontend team, drawing on a very large
skills pool, and it produces a modern UX (the model-detail, annotation, parameter-table and
diagram screens are already heavy client-side JS and would genuinely improve).

---

## 2. Key differences at a glance

| Dimension | Grails 7 full-stack re-platform | Decoupled Spring Boot + React |
|---|---|---|
| **Codebase churn** | Backend + views ported; ~0% thrown away | Backend ported; **frontend 100% rewritten** |
| **Long-term hiring pool** | Small (Grails is niche) | Large (Spring Boot + React are the two biggest) |
| **Incremental delivery** | Hard — one deploy unit, near-big-bang cutover | **Strong — strangle at the HTTP layer, page by page** (see §4) |
| **Infra complexity** | One repo, one pipeline, one artifact | Two repos, two pipelines, two artifacts, CORS, gateway routing, API versioning |
| **SEO / link previews / crawlers** | Free (server-rendered) | Needs SSR or prerendering for public model pages (§5) |
| **API surface** | Internal; `/api/*` stays partial | Must become the **complete, documented** contract for the whole app |
| **Auth model** | Session (web) + token (API), as today | One token/cookie model for the SPA; 2FA becomes an API flow (§6) |
| **Team topology** | One full-stack team | Separable backend / frontend teams |
| **Conventions lost** | none | GORM dynamic finders (if JPA), scaffolding, integrated view/controller, one-command run |
| **Total effort** | ~16–30 eng-months | Comparable total; **better parallelism** → similar or shorter calendar with 2 teams |
| **End state** | Modern framework, dated architecture (SSR monolith) | Modern framework **and** modern architecture; future upgrades are routine |

---

## 3. Effort comparison (order-of-magnitude)

| Bucket | Grails 7 | Decoupled |
|---|---|---|
| AnnotationStore + indexer + native deps | 1.5–3 mo | 1.5–3 mo (same; +0.5–1 mo if moving domain to JPA) |
| Backend services / Solr / Redis / Git / jobs / routes | 5–9 mo | 4–7 mo (no plugin restructuring, no GSP/asset work) |
| **Complete + documented REST API** (build out beyond today's partial `/api/*`) | — | **3–6 mo** (new work) |
| View layer | 4–8 mo (GSP → SiteMesh 3 + asset-pipeline) | **6–12 mo greenfield React** (parallelisable) |
| Test rewrite | 2–4 mo | 2–4 mo backend + new frontend test setup (1–2 mo) |
| Security workstream | 1.5–3 mo | 2–4 mo (auth-flow redesign for SPA) |
| Infra / cutover / Solr 9 | 1.5–3 mo | 2–4 mo (two pipelines, gateway, CDN/SSR) |
| **Total** | **~16–30 eng-months** | **~22–40 eng-months**, but split cleanly across two parallel teams → **calendar 12–20 months**, similar to or slightly longer than Grails 7, with a materially better end state and lower cutover risk |

The decoupled path costs *more total engineer-effort* (a full API + a full frontend rewrite)
but buys **parallelism**, **incremental go-live**, and an architecture that doesn't need
re-platforming again.

---

## 4. Migration sequencing — the decoupled path's biggest advantage

BioModels already exposes a partial `/api/*` block (model CRUD, OMEX, metadata, annotations,
submission, contributor management, search index, homepage widgets) and a
`ShowRestApiController`. That is a foothold for a **strangler-fig at the reverse proxy**:

```
              ┌─────────────────────────────┐
   traffic →  │  ingress / API gateway      │
              └─────────────────────────────┘
                 │ /api/**        │ /  (pages)
                 ▼                ▼
        ┌────────────────┐   route not-yet-migrated paths → OLD Grails 2 app
        │ Spring Boot API│   route migrated paths        → NEW React frontend
        └────────────────┘
```

1. **Phase A** — Stand up the Spring Boot backend re-serving today's `/api/*` surface
   (same DB, same Redis, same Solr, same Git repo). No behaviour change; old app still runs.
2. **Phase B** — Grow the API to cover a first vertical slice (e.g. search + model detail).
   Build those pages in React. Point `/search` and `/MODEL*` at the SPA via the gateway;
   everything else still hits Grails 2.
3. **Phase C..N** — Slice by slice: submission wizard, curation, contributor/team admin,
   user management, CMS pages, competitions. Retire Grails routes as React covers them.
4. **Final** — Grails 2 app has no traffic; decommission. **No big-bang cutover, no
   feature freeze on the whole product** — only the slice in flight is frozen.

Contrast: the Grails 7 path can't easily do this because the old and new apps can't co-serve
the same session/deploy unit, so it trends toward a single large cutover.

---

## 5. SEO / public-resource considerations (decoupled path)

BioModels is a public scientific data resource — model pages are indexed by Google, Google
Scholar, Europe PMC, and cited with stable URLs (`/MODEL...`, perennial IDs). A pure
client-rendered SPA degrades:

- search-engine indexing of model content,
- social / Slack / publication link previews (Open Graph),
- first-paint performance on slow connections,
- `curl`/script access to a human-readable page.

Mitigations (pick one):
- **Next.js with SSR/ISR** for the public read paths (model detail, search, browse,
  home, static pages); client-only rendering is fine for authenticated tools (submission,
  curation, admin).
- **Prerendering** (build-time or a prerender service) for the public paths if staying on a
  plain Vite SPA.
- Keep **content negotiation** on the backend: `/MODEL123` with `Accept: application/json`
  → API, with `Accept: text/html` → frontend; and serve `sitemap.xml` from the backend.

The Grails 7 path gives all of this for free — it's a genuine cost line for the split.

---

## 6. Auth / security differences (decoupled path)

| Aspect | Today (Grails) | Decoupled |
|---|---|---|
| Web auth | server session (Spring Session + Redis) | token in **httpOnly, Secure, SameSite cookie** (recommended) or `Authorization: Bearer` |
| API auth | `X-Auth-Token` GORM tokens | unify: same token model for SPA and third-party API clients |
| 2FA (JBM-689) | GSP pages + `VerifyOtpFilters` | backend returns a "2FA required" challenge; React renders the OTP form; backend verifies. Logic unchanged, presentation moves |
| Lockout (JBM-686) | `LoginAttemptCacheService` (Redis) | unchanged — stays in the backend |
| Device trust (30-day) | cookie set server-side | cookie/token issued by API |
| CSRF | Grails synchronizer token in forms | not needed with Bearer header; **needed** if using cookies → SameSite=strict + CSRF token endpoint |
| CORS | `cors` plugin on `/api/*` | first-class: frontend origin allow-list, credentials mode |
| Password hash migration (SHA-256 → bcrypt) | required either way | required either way — same risk |
| Third-party API clients (`biomodels-python-restful-client`, external) | already use `/api` | benefit from the now-complete, documented, versioned API |

Security review sign-off is needed on the new auth flow regardless of path; the decoupled
path changes *more* of it but lands on a more standard, better-understood model.

---

## 7. What you give up by leaving Grails

- **GORM dynamic finders / criteria conveniences** — if the backend moves to JPA. Keeping
  **GORM-standalone** on Spring Boot avoids this; recommended for phase 1 to limit blast
  radius, revisit JPA later.
- **Scaffolding & convention-over-configuration** — Spring Boot needs explicit wiring
  (component scan, `@Bean` config, `WebMvcConfigurer`). More boilerplate, all standard.
- **Integrated dev loop** — `./grailsw run-app` becomes "run backend + run frontend + proxy".
- **The Grails plugin ecosystem** — mail, quartz, spring-security wiring, cache: each
  replaced by a Spring Boot starter (all exist and are well-maintained).
- **GSP taglibs** — the 14 taglibs' logic moves into React components or backend response
  shaping.

---

## 8. Repository & tooling shape

| | Backend repo | Frontend repo |
|---|---|---|
| Language | Java 21 (or Kotlin) / optionally Groovy for GORM entities | TypeScript |
| Build | Gradle (Kotlin DSL), multi-module | Vite or Next.js, pnpm |
| Framework | Spring Boot 3.5, Spring Security 6, Spring Data / GORM-standalone | React 18/19, React Router or Next.js, React Query |
| Contract | publishes OpenAPI 3 spec (springdoc) | consumes generated TS client from that spec |
| CI | test + build + container image | lint + type-check + test + build + deploy to CDN |
| Deploy | container (K8s), fat JAR | static bundle on CDN, or Node runtime if SSR |
| Versioning | semver API; `/api/v1`, `/api/v2` | pinned to an API major; contract tests in CI |

**Monorepo alternative:** one repo with `/backend` and `/frontend` packages still lets each
side choose its stack, keeps cross-cutting changes atomic, and shares one CI — worth
considering if the team is small. The two-repo split pays off mainly when backend and
frontend teams are genuinely separate.

---

## 9. Recommendation

If the organisation is willing to invest in a **larger total effort for a materially better
end state**, the decoupled path is the stronger long-term choice for BioModels specifically:

- it fixes the *architecture*, not just the framework version — no third re-platform later;
- it draws on the two largest engineering talent pools;
- its **HTTP-layer strangler sequencing removes the big-bang cutover and the whole-product
  feature freeze**, which are the dominant *programme* risks in `03-risk-register.md` (G1);
- BioModels already has an API foothold and API consumers to justify a first-class API.

The costs to accept going in:
- a **complete, versioned, documented REST API** is now on the critical path;
- the **frontend is rebuilt from zero** (455 GSPs → React);
- **SSR/prerendering** is required for the public model pages (SEO / link previews);
- **more infra**: two pipelines, gateway routing, CORS, CDN, API contract testing.

Suggested decision gate at P0: prototype the strangler proxy + one React slice (search) on
top of a minimal Spring Boot backend re-serving `/api/model/*`, and measure the real cost of
that vertical before committing the programme.
