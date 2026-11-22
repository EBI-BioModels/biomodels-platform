# Search settings

BioModels searches models through EBI Search when `jummp.search.strategy` is `omicsdi`
(`OmicsdiBasedSearch`). The settings below change how it does that. They only matter for the
`omicsdi` strategy.

## Where to set them

In the external properties file that `Config.groovy` loads: `~/.jummp.properties`, or the file named by
the `JUMMP_CONFIG` environment variable. The application reads it at startup, so restart it after a change.

| Setting | Default | Meaning |
|---|---|---|
| `jummp.search.omicsdi.baseUrl` | `https://www.ebi.ac.uk` | The EBI Search server that model search queries. |
| `jummp.search.omicsdi.connectTimeoutMs` | `5000` | How long to wait for the connection to EBI Search. |
| `jummp.search.omicsdi.readTimeoutMs` | `45000` | How long to wait for the answer before giving up. |
| `jummp.search.omicsdi.firstAttemptTimeoutMs` | `8000` | How long the first attempt waits before the search is tried once more. A value that is not below `readTimeoutMs` turns the retry off. |

**Only `baseUrl` is copied from the properties file today.** `Config.groovy` has no line for the three
timeout settings, so as far as we can tell they do not take effect when set there. Copying them is a small
change in `Config.groovy`, not done yet.

## `jummp.search.omicsdi.baseUrl`

Use it to point search at another EBI Search server, for example `http://localhost:8000`. Without it
the application uses `https://www.ebi.ac.uk` in every environment, development included.
`wwwdev.ebi.ac.uk` is no longer used: its `biomodels` domain holds no models.

Valid values are `http` or `https`, a host name, IPv4 address or bracketed IPv6 address, an optional
numeric port, and nothing else:

- accepted: `https://www.ebi.ac.uk`, `http://localhost:8000`, `http://search_server:8000`, `http://[::1]:8000`
- refused: no scheme (`www.ebi.ac.uk`), a path (`https://www.ebi.ac.uk/ebisearch`), a query or fragment,
  an empty host (`http://:8000`), a port that is not a number (`http://host:abc`), credentials
  (`https://user:pw@host`)

The value is checked once at startup (`BootStrap`). A refused value stops the application with a message
that names the setting; credentials are never repeated in it. At startup the application prints the server it
uses: `Searching models on the EBI Search server https://www.ebi.ac.uk`.

## When EBI Search is slow

EBI Search sometimes answers in about 0.1 s and sometimes takes seconds or does not answer. The first attempt
waits `firstAttemptTimeoutMs`. If it times out while waiting for the answer, the search is tried once more
with `readTimeoutMs`. Refused connections, error answers (5xx) and rejected queries (4xx) are not retried. If
the second attempt fails too, the search page shows "Search is temporarily unavailable", so that takes up to
`firstAttemptTimeoutMs` plus `readTimeoutMs` (about 53 s with the defaults).

History: JBM-829 (server and setting), JBM-830 (whether search stays on EBI Search).
