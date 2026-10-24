# Running Tests

## All tests (unit + integration)
```bash
./grailsw test-app
```

## Unit tests only
```bash
./grailsw test-app unit:
```

## Integration tests only
```bash
./grailsw test-app integration:
```

## A specific test class
Works for either type — Grails infers it from where the class lives:
```bash
./grailsw test-app net.biomodels.jummp.core.model.identifier.ModelIdentifierGeneratorFactoryBeanSpec
```

## A specific test method within a class
```bash
./grailsw test-app net.biomodels.jummp.core.model.identifier.ModelIdentifierGeneratorFactoryBeanSpec."test injection of model identifier generator factory bean"
```

## Unit tests under one package/plugin only
Faster iteration while working in one area:
```bash
./grailsw test-app unit: net.biomodels.jummp.model.*
```

## A plugin's own tests
Each plugin has its own `grailsw`, run from inside the plugin dir:
```bash
cd jummp-plugins/jummp-plugin-omicsdi && ./grailsw test-app
```

## With coverage
```bash
./grailsw test-app -coverage && ./grailsw coverage-report
```
Report lands in `target/test-reports/`, coverage HTML under `target/test-reports/cobertura/`.

## Current status (as of 2026-09-06)
- Unit tests are green (89/89), following the JBM-350 test-suite repair work.
- Integration tests currently run (109 tests execute — an earlier fatal bootstrap crash caused by
  test-induced Redis key corruption is fixed), but 76 still fail. This is tracked as follow-up
  work in **JBM-745** (subtask of JBM-350).
- Full per-test results after any run are in `target/test-reports/plain/*.txt`.
