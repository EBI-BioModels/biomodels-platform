# Static assets settings

Logos, icons, screenshots and a few documents (such as the privacy notice) are not part of the application.
They are served under one base URL, for example by an Apache or Nginx server.

| Setting | Default | Meaning |
|---|---|---|
| `jummp.web.staticAssets.server` | see below | The server and path that serve the static assets, such as `https://assets.example.org/static-assets`. The application adds nothing but the file path, so a host name alone only works if the files are at its root. |

Set it in the external properties file that `Config.groovy` loads (`~/.jummp.properties`, or the file named
by the `JUMMP_CONFIG` environment variable), then restart the application:

```properties
jummp.web.staticAssets.server=https://assets.example.org/static-assets
```

Without the setting:

- if the server URL contains `biomodels.org`, `biomodels.net` or `dmscambs.co.uk`, the application uses
  `<server URL>/static-assets`;
- otherwise, development included, it uses `https://www.biomodels.org/static-assets`.

## Requirements for the server

- Give an absolute `http` or `https` URL. The application downloads the privacy notice from it, so a
  relative path does not work. The value is not checked at startup.
- The server must keep the same layout below the base URL (`images/`, `icons/`, `GRAPHICS/`, `info/`).
  A trailing slash is ignored.
- Browsers load most assets directly, so the URL must be reachable by users, and it must use `https` when
  the application does.
