# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BioModels is the world's largest repository of mathematical models of biological and biomedical systems (2,700+ published models). Built on the [Jummp](https://bitbucket.org/jummp/jummp/wiki/) infrastructure, it provides versioned model storage, multi-format submission (SBML, COMBINE Archive, PharmML, MDL), collaborative curation, and faceted search powered by Solr.

**Production site:** https://www.biomodels.org

**Tech stack:** Grails 2.5.5 (Groovy/Java), GORM/Hibernate, PostgreSQL/MySQL, Apache Solr 5.4.1, Spring Security, Quartz, Git-backed VCS, Tomcat 7 (production), Docker.

## Institutional Context

BioModels was developed and hosted at EMBL-EBI (European Bioinformatics Institute) for ~20 years with European Commission funding. In 2025 it migrated to the **Laboratory for Systems Medicine (LSM)**, Department of Medicine, Division of Pulmonary – Systems Medicine, University of Florida.

- LSM BioModels page: https://systemsmedicine.pulmonary.medicine.ufl.edu/biomodels/
- Contact: DOM-LabforSysMedicin@ad.ufl.edu
- EBI migration announcement: https://www.ebi.ac.uk/about/news/updates-from-data-resources/biomodels-moves-university-of-florida/

## Security Enhancement Initiative (2026)

A mandatory security rollout went live **04 May 2026**, announced to all users by email. Features added:

| Feature | Detail |
|---|---|
| Two-Factor Authentication (2FA) | Email-based OTP at login; optional 30-day device trust (JBM-689) |
| Smart Account Lockout | 1-hour lock after 3 consecutive failed login attempts (JBM-686) |
| Compromised Password Monitoring | Cross-checks passwords against known breach databases |
| Updated Password Standards | Stricter complexity and history rules |

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
- Keep subjects concise; put detail in the email body

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

# Run tests for a specific plugin (each plugin has its own grailsw)
cd jummp-plugins/jummp-plugin-<name> && ./grailsw test-app <FullyQualifiedTestClassName>
```

Test reports go to `target/test-reports/`. Coverage reports use Cobertura.

Dependencies are resolved via Maven (configured in `grails-app/conf/BuildConfig.groovy`). If a private Artifactory instance is available, set `JUMMP_ARTIFACTORY_URL` in the environment.

## Architecture

### Plugin-based modular structure

The application is split into a core Grails app (`grails-app/`) and 23+ plugins under `jummp-plugins/`. Each plugin is a self-contained Grails plugin with its own controllers, services, domain classes, and tests.

Key plugins:
- `jummp-plugin-core-api` — shared interfaces and DTOs used across plugins
- `jummp-plugin-security` — Spring Security configuration and 2FA support
- `jummp-plugin-git` — Git-backed versioned file storage for models
- `jummp-plugin-sbml` — SBML format parsing and validation (via JSBML)
- `jummp-plugin-combine-archive` — COMBINE Archive format support
- `jummp-plugin-solr` — Solr search integration
- `jummp-plugin-configuration` — externalised application configuration
- `jummp-plugin-jms` — JMS messaging (background job coordination)
- `jummp-plugin-annotation-*` — biological entity annotation

### Request flow

URL routing is defined in `grails-app/conf/UrlMappings.groovy`. Incoming requests go to controllers in `grails-app/controllers/net/biomodels/jummp/`, which delegate to services in `grails-app/services/net/biomodels/jummp/core/`. Key services:

- `ModelService` — model lifecycle (create, update, publish, delete)
- `SubmissionService` — multi-step model submission workflow
- `SearchService` — Solr-backed search and faceting
- `VcsService` — version control operations (delegates to git plugin)
- `FileSystemService` — file storage and retrieval

### Domain model

GORM domain classes in `grails-app/domain/net/biomodels/jummp/` represent: `Model`, `Revision`, `Publication`, `Person`, `Team`, `RepositoryFile`.

### Configuration

- `grails-app/conf/Config.groovy` — main app config (Spring Security, CORS, logging, mail)
- `grails-app/conf/DataSource.groovy` — database connections (env-aware)
- `grails-app/conf/BuildConfig.groovy` — dependencies and build settings
- `grails-app/conf/BootStrap.groovy` — startup initialisation

### Testing

Tests use the Spock framework (`.groovy` files in `test/unit/` and `test/integration/`). Unit tests use Grails unit testing mixins; integration tests require a running Spring context. Test fixtures are in `test/files/`.

### Commit style

Commits follow the pattern `JBM-### type: message` where type is `fix`, `feat`, `chore`, or `docs`, e.g. `JBM-686 fix: skip attempt counting for non-existing users`.
