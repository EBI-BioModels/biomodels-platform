# Creating a personal Bitbucket API token

`biomodels/jummp-biomodels`'s `origin` remote is Bitbucket Cloud
(`git@bitbucket.org:biomodels/jummp-biomodels.git`), not GitHub — there is no `gh` CLI
support, so PR automation goes through the Bitbucket REST API directly.

The repo/workspace access tokens (`BITBUCKET_ACCESS_TOKEN` / `BITBUCKET_API_TOKEN`) work
for plain repo read/write, but they are **not** tied to a real Bitbucket login — `GET
/2.0/user` rejects them with `"This API is not accessible by this authentication
mechanism"`, and any PR opened with one is attributed to the token itself, not to a person.

To have PRs (or other API calls) show up as authored by **you**, create a personal, scoped
Atlassian API token instead and use it with HTTP Basic auth.

## Steps

1. Go to <https://id.atlassian.com/manage-profile/security/api-tokens>.
2. Click **Create API token**, then choose **"Create token with scopes"** (not the classic
   unscoped token).
3. Set the app to **Bitbucket** and select the scopes needed:
   - `read:pullrequest:bitbucket`, `write:pullrequest:bitbucket` — read/create/update PRs
   - `read:repository:bitbucket`, `write:repository:bitbucket` — repo read/write
   - `read:user:bitbucket` — optional; only needed if something calls `GET /2.0/user` to
     confirm the token's identity. Not required for opening PRs.
4. Name the token (e.g. `jummp-biomodels PR automation`) and create it.
5. Copy the token value immediately — it is shown only once.

## Storing it

Add to `~/Work/EBI/key.dat` (same file as the Jira/Bitbucket access tokens):

```
BITBUCKET_USER_EMAIL=<your Atlassian account email>
BITBUCKET_USER_API_TOKEN=<paste the token here>
```

Never commit this file or paste the token value anywhere else (chat, memory files, etc.) —
only read it live via `source ~/Work/EBI/key.dat` when needed.

## Using it

HTTP Basic auth, `email:token`, e.g. to list PRs:

```bash
source ~/Work/EBI/key.dat
curl -u "$BITBUCKET_USER_EMAIL:$BITBUCKET_USER_API_TOKEN" \
  "https://api.bitbucket.org/2.0/repositories/biomodels/jummp-biomodels/pullrequests"
```

Or to open one:

```bash
curl -u "$BITBUCKET_USER_EMAIL:$BITBUCKET_USER_API_TOKEN" \
  -X POST -H "Content-Type: application/json" \
  -d '{"title": "...", "description": "...",
       "source": {"branch": {"name": "<feature-branch>"}},
       "destination": {"branch": {"name": "development"}}}' \
  "https://api.bitbucket.org/2.0/repositories/biomodels/jummp-biomodels/pullrequests"
```

Verified 2026-09-14: a token with `read/write:pullrequest:bitbucket` and
`read/write:repository:bitbucket` successfully creates PRs attributed to the token owner's
real display name (not a bot identity), even without `read:user:bitbucket`.
