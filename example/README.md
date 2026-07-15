# IDto Android example

A one-tap app that runs a real IDto verification session. It hosts an
`IDtoLandingView`, fetches a short-lived `client_token` from `POST /auth/sdk/token`
via `SdkTokenClient`, opens the verification flow, and toasts every callback.

## Credentials

The demo needs a trio from IDto onboarding — `client_id`, `client_secret`, and a
`workflow_template_id`. They are **never committed**; the build injects them at
compile time from, in order:

1. **Environment variables** — `IDTO_DEMO_CLIENT_ID`, `IDTO_DEMO_CLIENT_SECRET`,
   `IDTO_DEMO_WORKFLOW_ID`.
2. **Gradle properties** — the same keys, e.g. `-PIDTO_DEMO_CLIENT_ID=…`.
3. **`example/demo.properties`** — a git-ignored file (fastest for local runs).

Copy the template and fill it in:

```bash
cp example/demo.properties.example example/demo.properties
# then edit example/demo.properties
```

```properties
IDTO_DEMO_CLIENT_ID=<your client_id>
IDTO_DEMO_CLIENT_SECRET=<your client_secret>
IDTO_DEMO_WORKFLOW_ID=<your workflow_template_id>
```

> ⚠️ **Test-only.** A real app must never embed a `client_secret`; your backend
> holds it and returns only the short-lived `client_token`. The example exchanges
> the secret directly **only** because it talks to a throwaway sandbox and this
> module is not published.

If the trio is blank, the app opens to a toast — *"Set demo credentials — see
example/README"* — instead of the flow, and the `SmokeTest` **skips** rather than
fails.

## Run

```bash
./gradlew :example:installDebug   # real device recommended for camera steps
```

Tap **Start verification**. The app fetches a `client_token`, opens the flow for
your `workflow_template_id`, and toasts each `onStepComplete` / `onComplete` /
`onAbandon` / `onError`.

## Provision your own sandbox client

```bash
# 1) Sign up a dev customer
curl -s -X POST https://dev.idto.ai/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","password":"YourPassw0rd!"}'

# 2) Log in → capture the customer JWT
curl -s -X POST https://dev.idto.ai/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","password":"YourPassw0rd!"}'

# 3) Create a client → capture client_id + client_secret (shown once)
curl -s -X POST https://dev.idto.ai/customers/create-client \
  -H "Authorization: Bearer <CUSTOMER_JWT>" \
  -H 'Content-Type: application/json' \
  -d '{"name":"android-example"}'

# 4) Create / pick a workflow_template_id in the dashboard, then put all three
#    into example/demo.properties (and set env to DEVELOPMENT in DevCredentials).
```

Verify the trio works before running:

```bash
TOKEN=$(curl -s -X POST https://dev.idto.ai/auth/sdk/token \
  -H 'Content-Type: application/json' \
  -d '{"client_id":"<ID>","client_secret":"<SECRET>"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')

curl -s -X POST https://dev.idto.ai/sdk/v2/session/init \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"workflow_template_id":"<WORKFLOW>","start_fresh":true}'
```

Non-secret demo config (base URL, env, business name, brand color, sample phone)
lives in `DevCredentials`; point it at a local backend by setting `BASE_URL` and
`ENV = IDtoEnv.DEVELOPMENT`.
