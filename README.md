# IDto Android SDK

Add **IDto identity verification (KYC)** to your native Android app. The SDK runs the
entire flow — mobile OTP, Aadhaar, PAN, face match + liveness, bank verification, and
more — in the exact order your workflow defines. Your users get a guided, branded
experience; you get verified results through typed Kotlin/Java callbacks.

Under the hood it hosts the official IDto **web SDK CDN bundle** (`idto.js`) inside an
Android `WebView` and exposes a small, Java-first surface:

```kotlin
// Low-level: you own the trigger; the SDK owns the screen.
IDto.open(context, config, listener)   // start the flow

// Batteries-included: a polished landing screen + token lifecycle + sheet.
IDtoLandingView(context).apply { configure(landingConfig); setListener(listener) }
```

The web SDK is **never forked**. The wrapper loads the same rolling CDN bundle the web
integration uses (selected by `env`), so new web SDK releases — new modules, fixes, UI —
flow through automatically. This library only owns the typed config surface, the callback
bridge, and the native `WebView` host: camera/mic permissions, DigiLocker redirects, a
navigation allow-list, and native report sharing. Unlike the web integration, there is no
CSP to configure and no CORS exception to add — the `WebView` runs same-origin under the
IDto API host (see [Security notes](#security-notes)).

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Camera & microphone permissions](#camera--microphone-permissions)
- [Quick start](#quick-start)
- [How it works](#how-it-works)
- [Configuration reference](#configuration-reference)
- [Callbacks & result](#callbacks--result)
- [Step results](#step-results)
- [Verification outcome](#verification-outcome)
- [Credits](#credits)
- [Resuming a session](#resuming-a-session)
- [Pre-filling data](#pre-filling-data)
- [Session statuses](#session-statuses)
- [IDtoLanding](#idtolanding)
- [Report download](#report-download)
- [Token auto-refresh](#token-auto-refresh)
- [Verification modules](#verification-modules)
- [Environments](#environments)
- [Server-side verification](#server-side-verification)
- [DigiLocker](#digilocker)
- [Security notes](#security-notes)
- [Example app](#example-app)
- [Versioning & web-SDK parity](#versioning--web-sdk-parity)
- [Troubleshooting](#troubleshooting)

## Features

- **One entry point, typed end-to-end.** `IDto.open` starts the flow; every config field is
  a fluent builder method and every callback payload is a typed class — no stringly maps.
  Works identically from Kotlin, Java, Compose, and View hosts.
- **Full module coverage** via the web SDK: mobile OTP, Aadhaar (DigiLocker / OKYC), PAN,
  face match + liveness, bank account, driving licence, vehicle RC, and silent name match.
- **SDK-owned camera & microphone permissions.** The library declares `CAMERA` /
  `RECORD_AUDIO` and requests them at runtime **before** the `WebView` opens — no
  black-screen race, nothing for the host to wire up.
- **DigiLocker OAuth in-app** — the `window.open` flow completes **inside** a nested
  `WebView` and returns to the session, no system-browser hop.
- **Native report sharing built in** — the result PDF is captured and handed to the OS
  share/save sheet automatically, **no optional dependencies**.
- **Session resume** — pass a `sessionToken` to continue an interrupted flow.
- **Token auto-refresh** — supply an `IDtoTokenProvider` and an expired bearer is replaced
  transparently mid-session; the user sees nothing.
- **Branding & theming** — colors, logo, light/dark, English/Hindi, full-screen or native
  bottom-sheet display.
- **Hardened host** — an in-`WebView` navigation allow-list (extensible via `allowedHosts`),
  a configurable `readyTimeoutMs`, and deterministic init-failure handling.

## Requirements

| Item | Value |
| ---- | ----- |
| `minSdk` | 24 (Android 7.0) |
| `compileSdk` / `targetSdk` | 36 |
| JDK | 17 |
| Language | Kotlin or Java host — both first-class |
| Device | A **real device** for end-to-end face match / document capture; most emulators have no usable camera. |

To integrate you also need, from IDto onboarding:

- An **IDto account** with a `client_id` / `client_secret` pair — the secret must **never**
  ship in the app.
- A **workflow template UUID** (`workflowTemplateId`) — it decides which modules run, in
  what order.
- A **backend endpoint** that mints a short-lived `clientToken` via `POST /auth/sdk/token`.
- A **sandbox** for development — set `IDtoEnv.DEVELOPMENT` (points at `https://dev.idto.ai`).

## Installation

The library is published as an Android AAR under the coordinates
`ai.idto:idto-android:0.1.0`. Two consumption paths are supported.

### Via JitPack (available now)

Add the JitPack repository, then depend on the tagged release. **Kotlin DSL**
(`settings.gradle.kts`):

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("com.github.idto-ai:idto-android:0.1.0")
}
```

**Groovy DSL** (`settings.gradle`):

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

```groovy
// app/build.gradle
dependencies {
    implementation 'com.github.idto-ai:idto-android:0.1.0'
}
```

### Via the `ai.idto` Maven coordinates

Once the artifact is published to a Maven repository you can control (Maven Central or a
private repo), depend on it directly:

```kotlin
dependencies {
    implementation("ai.idto:idto-android:0.1.0")
}
```

> **Availability note:** `ai.idto:idto-android` is the canonical coordinate;
> `com.github.idto-ai:idto-android` is what JitPack serves today from the version tag. Both
> resolve to the same AAR — see [`PUBLISHING.md`](PUBLISHING.md).

### Java hosts

No Kotlin setup is required in a pure-Java app. The listener interfaces
(`IDtoEventListener`, `IDtoLandingListener`) compile to real **Java default methods**, so
you override only the callbacks you need. All builders and `IDto.open` are plain
static/instance methods usable from Java verbatim (see the [Quick start](#quick-start)
Java snippet).

## Camera & microphone permissions

The flow uses the **camera** to capture documents and the user's face, and the
**microphone** only if your workflow includes a liveness step. The SDK opens the right
camera automatically — the **rear camera for documents**, the **front camera for
selfies** — so there is nothing to configure.

**The library owns the permissions.** It declares `CAMERA` and `RECORD_AUDIO` in its own
manifest (they merge into your app), and `IDto.open` requests any missing runtime
permission **before** creating the `WebView`. You do **not** add manifest entries and you
do **not** call a permission API yourself.

> This is an improvement over the IDto **Flutter** SDK, where the host app must declare the
> permissions and request them (via `permission_handler`) before opening, or the camera
> step shows a black screen. On Android the native SDK does both for you.

### What your user sees

The first time a camera step is reached, the OS shows a one-time prompt like
**"Allow <App> to use the camera?"**. On **Allow**, the flow proceeds; if your workflow
runs liveness, the user also gets a one-time **microphone** prompt. The session proceeds
regardless of the answer — not every workflow uses the camera.

If the user selects **Don't allow** and Android stops asking ("permanently denied"), the
SDK detects it and, when the web layer later needs the camera, shows a one-time dialog
offering **Open Settings**. After the user enables the permission and returns, the next
camera attempt succeeds — no restart. Declining falls through to the web SDK's own camera
error; the session continues either way.

## Quick start

### Step 1 — mint a client token on your backend

The `clientToken` is short-lived and comes from **your** backend, which exchanges your
`client_id` / `client_secret` at `POST /auth/sdk/token`. Never embed the secret in the app
(see [Security notes](#security-notes)).

```
POST https://prod.idto.ai/auth/sdk/token
Content-Type: application/json

{ "client_id": "…", "client_secret": "…" }
→ { "access_token": "…" }        // return only this to the app as the clientToken
```

### Step 2 — open the flow (Kotlin)

```kotlin
import ai.idto.sdk.IDto
import ai.idto.sdk.IDtoConfig
import ai.idto.sdk.IDtoEnv
import ai.idto.sdk.IDtoEventListener
import ai.idto.sdk.IDtoErrorData
import ai.idto.sdk.IDtoStepCompleteData
import ai.idto.sdk.IDtoWorkflowCompleteData

val config = IDtoConfig.Builder(clientToken, "your-workflow-uuid")
    .businessName("Acme Lending")
    .env(IDtoEnv.DEVELOPMENT)          // PRODUCTION (default) | DEVELOPMENT
    .build()

IDto.open(this, config, object : IDtoEventListener {
    override fun onStepComplete(data: IDtoStepCompleteData) {
        // data.step, data.result, data.creditsDeducted, data.balanceRemaining
    }
    override fun onWorkflowComplete(data: IDtoWorkflowCompleteData) {
        // POST data.sessionToken to your backend to confirm server-side.
    }
    override fun onError(data: IDtoErrorData) { /* data.step, data.error */ }
    override fun onClose() { /* widget torn down — fires exactly once */ }
})
```

### Step 2 — open the flow (Java)

```java
import ai.idto.sdk.IDto;
import ai.idto.sdk.IDtoConfig;
import ai.idto.sdk.IDtoEnv;
import ai.idto.sdk.IDtoEventListener;
import ai.idto.sdk.IDtoErrorData;
import ai.idto.sdk.IDtoWorkflowCompleteData;

IDtoConfig config = new IDtoConfig.Builder(clientToken, "your-workflow-uuid")
        .businessName("Acme Lending")
        .env(IDtoEnv.DEVELOPMENT)
        .build();

IDto.open(this, config, new IDtoEventListener() {
    @Override public void onWorkflowComplete(IDtoWorkflowCompleteData data) {
        // POST data.getSessionToken() to your backend to confirm server-side.
    }
    @Override public void onError(IDtoErrorData data) { /* data.getError() */ }
    @Override public void onClose() { }
});
```

`IDto.open` starts a translucent host Activity, so it works from any `Context` (Activity,
Compose `ComponentActivity`, or a Service). Call `IDto.close()` to dismiss programmatically
and `IDto.isOpen()` to check whether a session is live. Opening a second session while one
is active delivers `onError` (`step = "init"`, `error = "session_active"`) to the new
listener and is otherwise a safe no-op.

### Step 3 — confirm server-side

On `onWorkflowComplete`, send the `sessionToken` to your backend and verify it against
the state API before granting access — see [Server-side verification](#server-side-verification).

## How it works

```
Your backend                 Your Android app                 IDto
────────────                 ────────────────                 ────
client_id + secret
   │ POST /auth/sdk/token
   └───────────────────────────────────────────────────────────▶ clientToken
                                   │ IDto.open(context, config, listener)
                                   │   loads CDN bundle in a WebView
                                   │   runs the workflow's modules
                                   │◀── onStepComplete (per step)
                                   │◀── onWorkflowComplete / onError / onAbandon
                                   ▼
                             onClose (widget torn down)
   GET /sdk/v2/session/<token>/state  ◀── confirm server-side before granting access
```

1. Your **backend** mints a short-lived `clientToken` from `/auth/sdk/token`; the secret
   never leaves the server.
2. Pass that token to `IDto.open`. The SDK requests camera/mic permissions, hosts the CDN
   bundle in a `WebView`, and runs the workflow.
3. The listener streams `onStepComplete` per step, then `onWorkflowComplete` / `onError` /
   `onAbandon`, and finally `onClose`. Your backend **confirms** the `sessionToken`
   server-side before granting access.

`onWorkflowComplete` is **non-terminal** — the sheet stays up for the web success/report
screen. `onClose` is the **only** terminal event and always fires exactly once. Every
dismiss path (back press, backdrop tap, `IDto.close()`) routes through the web SDK's own
close handshake before teardown.

## Configuration reference

Build an `IDtoConfig` with `IDtoConfig.Builder(clientToken, workflowTemplateId)`. Only
those two constructor arguments are required; every method below is optional.

### Core & session

| Builder method | Wire key | Type | Description |
| -------------- | -------- | ---- | ----------- |
| `sessionToken` | `session_token` | `String` | Resume an in-progress session. See [Resuming a session](#resuming-a-session). |
| `merchantUserId` | `merchant_user_id` | `String` | Your internal user id (shown in the dashboard). |
| `phone` | `phone` | `String` | Pre-fills the mobile OTP step and de-duplicates sessions. |
| `startFresh` | `start_fresh` | `Boolean` | Force a new session even if one exists. |
| `preVerified` | `pre_verified` | `Map<String, Boolean>` | Module slugs already verified, to skip those steps. |
| `accumulatedData` | `accumulated_data` | `JSONObject` | Prior-session data to rehydrate (`merchant_injects` mode). |
| `referenceName` | `reference_name` | `String` | Reference name for the silent `name_match` module. |
| `env` | — | `IDtoEnv` | `PRODUCTION` (default) or `DEVELOPMENT`. Selects the CDN bundle, API base, and `WebView` origin. |
| `baseUrl` | `baseUrl` | `String` | Advanced: override the IDto API base URL; the `WebView` origin follows it (calls stay same-origin). |

### Branding & display

| Builder method | Type | Description |
| -------------- | ---- | ----------- |
| `businessName` | `String` | Shown in the widget header. |
| `logo` | `String` | Absolute HTTPS logo URL (loaded inside the `WebView` header). |
| `theme` | `IDtoTheme` | `LIGHT` (default) or `DARK`. |
| `language` | `IDtoLanguage` | `EN` (default) or `HI`. |
| `displayMode` | `IDtoDisplayMode` | `FULL_SCREEN` (default) or `BOTTOM_SHEET`. The sheet is drawn natively over a dim backdrop at a fixed 90% height. |
| `bottomSheet` | `IDtoBottomSheet` | Native sheet sizing for `BOTTOM_SHEET` mode (see below). |
| `colors` | `IDtoColors` | Color overrides (see below). |

**`IDtoColors`** — build with `IDtoColors.Builder()`. All optional: `background`, `text`,
`text2`, `border`, `primary`, `secondary`, `buttonTextColorPrimary`,
`buttonTextColorSecondary` (the last two serialize to the wire keys
`buttonTextColor_primary` / `buttonTextColor_secondary`).

**`IDtoBottomSheet`** — build with `IDtoBottomSheet.Builder()`; `minHeight` and `maxHeight`
each accept a `String` (e.g. `"60%"`) or an `Int` (px). `minHeight` sets the native sheet
height (default `"90%"`); `maxHeight` is accepted for web parity but not applied by the
native shell.

### Module configuration

| Builder method | Type | Description |
| -------------- | ---- | ----------- |
| `aadhaarConfig` | `IDtoAadhaarConfig` | See below. |
| `panConfig` | `IDtoPanConfig` | `skipContextScreen` — skip the PAN consent screen (you collect consent yourself). |
| `faceMatchConfig` | `IDtoFaceMatchConfig` | See below. |
| `faceMatchReferenceImage` | `String` | HTTPS URL or base64 data URI to match the captured face against. |
| `nameMatchConfig` | `IDtoNameMatchConfig` | See below. |

**`IDtoAadhaarConfig`** — `digilockerMaxFailures` (`Int`, DigiLocker failures before
fallback) and `okycEnabled` (`Boolean`, enable Aadhaar OTP fallback — **non-compliant**,
enable only with IDto approval).

**`IDtoFaceMatchConfig`** — `skipLiveness` (`Boolean`), `threshold` (`Int`, 0–100 minimum
match percentage), and `livenessFailurePolicy` (`IDtoLivenessPolicy`: `FAIL_OPEN`,
`FAIL_CLOSED`, or `NEEDS_REVIEW`, serialized as `fail_open` / `fail_closed` /
`needs_review`) — the policy only applies when `skipLiveness` is `false`.

**`IDtoNameMatchConfig`** — `threshold` (`Int`), `decisionMode` (`IDtoDecisionMode`: `ALL`
or `ANY`, wire `decision_mode`), `extraHonorifics` (`List<String>`, wire
`extra_honorifics`), `aliases` (`Map<String, String>`), and `comparePairs`
(`List<IDtoComparePair>`, wire `compare_pairs`; values `AADHAAR_VS_PAN`, `AADHAAR_VS_DL`,
`AADHAAR_VS_BANK`, `PAN_VS_BANK`).

### Host-only options

These tune the native host and are **not** sent to the web SDK.

| Builder method | Type | Default | Description |
| -------------- | ---- | ------- | ----------- |
| `debug` | `Boolean` | `false` | Forward in-`WebView` `console` logs and blocked-navigation notices to Logcat. |
| `readyTimeoutMs` | `Long` | `30000` | How long to wait for the bundle to signal ready before failing with `network_error`. |
| `allowedHosts` | `List<String>` | — | Extra hostnames the `WebView` may navigate to, on top of the IDto API host, the CDN bucket, and DigiLocker. |

## Callbacks & result

Implement `IDtoEventListener` (all methods have empty defaults, so override only what you
need):

| Callback | Payload | When |
| -------- | ------- | ---- |
| `onStepComplete` | `IDtoStepCompleteData` | After each workflow step. |
| `onWorkflowComplete` | `IDtoWorkflowCompleteData` | All steps finished. **Non-terminal.** Confirm `sessionToken` server-side. |
| `onError` | `IDtoErrorData` | An error at a step (`init` failures tear the session down; others may recover). |
| `onAbandon` | `IDtoAbandonData` | User abandoned mid-flow. |
| `onClose` | _(none)_ | Widget dismissed / torn down. **Terminal** — fires exactly once. |

All callbacks are delivered on the **main thread**. The payload classes expose camelCase
getters over the web SDK's verbatim data:

```kotlin
class IDtoStepCompleteData(
    val step: String,
    val result: JSONObject,
    val accumulatedData: JSONObject,
    val sessionToken: String,
    val creditsDeducted: Double,
    val balanceRemaining: Double,
)
class IDtoWorkflowCompleteData(
    val allSteps: JSONArray,
    val accumulatedData: JSONObject,
    val sessionToken: String,
)
class IDtoAbandonData(val atStep: String, val reason: String, val sessionToken: String)
class IDtoErrorData(val step: String, val error: String, val sessionToken: String)
```

`result` and `accumulatedData` are raw `JSONObject`s — read richer per-module fields from
them directly ([Step results](#step-results)).

### `onError` — stable error strings

These SDK-generated `error` values are safe to branch on:

| `error` value | When it fires |
| ------------- | ------------- |
| `network_error` | The CDN bundle or a request failed. On init: `step = "init"`, `error = "network_error"`. |
| `session_expired` | The `clientToken` / session expired and no token provider recovered it. |
| `workflow_not_found` | `workflowTemplateId` does not exist in this environment. |
| `step_already_complete` | The step was completed in a prior session — resume with `sessionToken`. |
| `step_completion_failed` | Backend rejected step completion. |
| `insufficient_credits` | Credit balance reached zero — the flow halts and tears down. |
| `timeout` | The backend did not respond in time. |
| `unknown_error` | Catch-all (the bundle loaded without `IDtoSDK`, or `open` threw). |
| `session_active` | SDK-generated: a second `IDto.open` while a session is active (`step = "init"`). |

When `step == "init"`, the `error` may be a raw backend string (an invalid/expired token
message). Do not string-match those — log them and show a generic message.

## Step results

Each step's verified data arrives in the `onStepComplete` `result`, and the same data is
merged into `accumulatedData` (keyed by module slug). Shapes below are what the web SDK
emits:

```jsonc
// mobile_verification
{ "mobile_number": "9876543210", "verified": true }

// aadhar_verification
{ "name": "RAHUL SHARMA", "dob": "1990-01-15", "gender": "M",
  "masked_aadhaar": "XXXX-XXXX-1234", "verified": true }

// pan_verification
{ "pan_number": "ABCDE1234F", "name": "RAHUL SHARMA", "verified": true }

// face_match
{ "match_score": 0.97, "matched": true, "verified": true }

// account_verification
{ "account_number": "123456789012", "ifsc": "SBIN0001234", "verified": true }
```

**Name match** is a **silent** module: read it from
`accumulatedData.getJSONObject("name_match")` (in `onWorkflowComplete` or a later
`onStepComplete`), not from an individual step `result`, which is empty for silent
modules. It carries per-pair `score`s and — only when `nameMatchConfig` sets a
`threshold` — per-pair `match` booleans plus a top-level `passed`. It never blocks the
user.

## Verification outcome

A result carries a **verdict** separate from whether the user finished the steps. The
session `status` says *did the flow finish*; `outcome` says *what the verdict was*. A
session can be `completed` and still `not_verified`.

The `outcome` is surfaced verbatim on the web SDK data and on the state API. It is one of:

| `outcome` | Meaning |
| --------- | ------- |
| `verified` | All checks passed. |
| `needs_review` | A soft flag (e.g. a below-threshold name match or a liveness verdict routed to review). Queue for manual review. |
| `not_verified` | A hard check failed. |

**Gate access on `outcome == "verified"`**, not merely on the flow having finished — and
always confirm server-side ([Server-side verification](#server-side-verification)), since
the on-device callback can be tampered with before it reaches your server.

## Credits

Each step that performs a backend verification consumes credits, reported per step:

```kotlin
override fun onStepComplete(data: IDtoStepCompleteData) {
    Log.d("kyc", "step=${data.step} used=${data.creditsDeducted} left=${data.balanceRemaining}")
}
```

- Credits are in **credit units**, not currency — ask your integration manager for the
  mapping.
- `creditsDeducted` / `balanceRemaining` are **only** on `onStepComplete`, not on
  `onWorkflowComplete`.
- When the balance reaches zero, `onError` fires with `error = "insufficient_credits"` and
  the flow halts.

## Resuming a session

If a user closes the widget mid-flow, resume by passing the same `sessionToken` (saved
from a previous `onAbandon` / `onClose`) with a **fresh** `clientToken`:

```kotlin
val config = IDtoConfig.Builder(freshToken, "your-workflow-uuid")
    .sessionToken(savedSessionToken)   // resumes from the last incomplete step
    .build()
```

The SDK skips completed steps and lands the user on the next pending step. Pass
`startFresh` to force a brand-new session instead.

> **Storage warning:** if you persist `sessionToken` to resume across app launches, treat
> it as a secret — it is a bearer for that session. Prefer server-side session storage
> keyed to your authenticated user.

## Pre-filling data

Pass `phone` to pre-fill the mobile OTP step (also used for session de-duplication). For
workflows in **`merchant_injects` continuity mode** (where the server retains no PII),
rehydrate prior-step data via `accumulatedData` and skip already-verified modules via
`preVerified`:

```kotlin
val config = IDtoConfig.Builder(clientToken, "your-workflow-uuid")
    .phone("9876543210")
    .preVerified(mapOf("mobile_verification" to true))
    .accumulatedData(JSONObject().put("pan_verification", JSONObject().put("full_name", "RAHUL SHARMA")))
    .build()
```

Contact your IDto integration manager to confirm whether your account uses `merchant_injects` mode.

## Session statuses

The session lifecycle (on the state API and webhook `status` field):

| `status` | Meaning |
| -------- | ------- |
| `in_progress` | The user is actively moving through the steps. |
| `completed` | All steps are done. The verdict is in `outcome`. |
| `abandoned` | A zombie/inactive session reaped by the inactivity sweep. |
| `expired` | The session passed its hard TTL without completing. |

There is no `failed` status: a failed check is recorded at the step level and rolled up
into `outcome: not_verified`.

## IDtoLanding

`IDtoLandingView` is a batteries-included landing screen — header, hero, numbered steps,
CTA, trust row, footer — that owns the token lifecycle and opens the flow as a native
bottom sheet. You supply a `workflowTemplateId`, an `IDtoTokenProvider`, and branding.

```kotlin
import ai.idto.sdk.landing.IDtoLandingConfig
import ai.idto.sdk.landing.IDtoLandingListener
import ai.idto.sdk.landing.IDtoLandingView

val landing = IDtoLandingView(this)
landing.configure(
    IDtoLandingConfig.Builder("your-workflow-uuid", tokenProvider)
        .businessName("Acme Lending")
        .brandColor("#0019FF")
        .build()
)
landing.setListener(object : IDtoLandingListener {
    override fun onComplete(data: IDtoWorkflowCompleteData) { /* confirm server-side */ }
    override fun onDismiss() { /* the sheet closed */ }
})
setContentView(landing)
```

`IDtoLandingConfig.Builder` forwards the same module/branding options as `IDtoConfig`
(`aadhaarConfig`, `faceMatchConfig`, `nameMatchConfig`, `panConfig`, `theme`, `language`,
`phone`, `env`, `colors`, …), plus landing-specific extras: `brandColor` (drives the CTA
and palette), `logo` (a `Drawable` for the native landing header), `logoSizeDp(width,
height)`, `logoUrl` (a URL for the in-sheet web header, distinct from `logo`), and `copy`
(an `IDtoLandingCopyOverride` to override any landing text). `IDtoLandingListener` exposes
`onComplete`, `onDismiss`, `onAbandon`, `onError`, and `onStepComplete`.

### Compose

`IDtoLandingView` is a plain `View`, so host it with `AndroidView`:

```kotlin
AndroidView(factory = { ctx ->
    IDtoLandingView(ctx).apply {
        configure(IDtoLandingConfig.Builder("your-workflow-uuid", tokenProvider).build())
        setListener(landingListener)
    }
})
```

Branding defaults live in `LandingDefaults` (`IDTO_BRAND`, `DEFAULT_COPY`) for matching the palette in a custom landing.

## Report download

The flow can let the user download the result report (PDF). Inside a `WebView` the
browser's own `<a download>` does nothing, so the SDK intercepts the blob, stages it in the
app cache, hands it to the OS share/save sheet via a library-owned `FileProvider`, then
deletes the staged file (PII cleanup). This is **built in** — no optional dependencies,
unlike the React Native SDK. Share dismissal is not an error.

## Token auto-refresh

The `clientToken` is short-lived. Supply an `IDtoTokenProvider` and the SDK swaps in a
fresh token transparently when the in-`WebView` bundle needs one — the user sees nothing.

```kotlin
val tokenProvider = IDtoTokenProvider { callback ->
    Thread {
        try {
            callback.onToken(mintClientTokenFromYourBackend())  // returns a String
        } catch (t: Throwable) {
            callback.onError(t)
        }
    }.start()
}

IDto.open(this, config, listener, tokenProvider)
```

`IDtoTokenProvider` is a functional interface (`getToken(callback: IDtoTokenCallback)`);
answer asynchronously on any thread via the callback's `onToken` or `onError`. In Java it
is a SAM you can pass as a lambda. Mint the token on **your** server — never ship
`client_secret`. The same provider passed to `IDto.open` (or configured on
`IDtoLandingConfig`) drives refresh; omit it and an expired token surfaces as `onError`.

## Verification modules

The workflow referenced by `workflowTemplateId` decides which modules run and in what
order (configured in the IDto dashboard). If your workflow includes an **e-sign** step,
the SDK opens the signing interface inline in the `WebView` — no extra host code; the
document endpoint is provisioned per-client during onboarding.

| Module | Slug | What the user does |
| ------ | ---- | ------------------ |
| Mobile OTP | `mobile_verification` | Enters phone, verifies an OTP. |
| Aadhaar | `aadhar_verification` | DigiLocker OAuth (or OKYC / upload fallback). |
| PAN | `pan_verification` | Enters PAN; verified against source. |
| Face match | `face_match` | Captures a selfie; optional liveness. |
| Bank account | `account_verification` | Verifies a bank account / IFSC. |
| Driving licence | `driving_licence` | Verifies a DL. |
| Vehicle RC | `vehicle_rc` | Verifies a vehicle RC. |
| E-sign | `e_sign` | Signs a document. |
| Govt ID selection | `govt_id_selection` | Chooses an ID method (router step). |
| Name match | `name_match` | **Silent** — compares names across sources; never blocks. |

## Environments

| `env` | CDN bundle | API base |
| ----- | ---------- | -------- |
| `PRODUCTION`\* | `…/idto-sdk-bucket/sdk/prod/idto.js` | `https://prod.idto.ai` |
| `DEVELOPMENT` | `…/idto-sdk-bucket/sdk/dev/idto.js` | `https://dev.idto.ai` |

\* default. `env` selects the CDN bundle, the API base, and the `WebView` document origin
(a secure https context, required for the camera); the origin follows `baseUrl` when set,
so the bundle's API calls stay same-origin.

## Server-side verification

On-device callbacks drive UX; they are **not authoritative** (the app can be backgrounded,
lose connectivity, or be tampered with). Confirm the truth server-to-server.

**State API poll:**

```
GET https://prod.idto.ai/sdk/v2/session/<sessionToken>/state
Authorization: Bearer <IDTO_API_SECRET>
```

The response includes `status` ([Session statuses](#session-statuses)), `outcome`
([the verdict](#verification-outcome)), and `accumulated_data` (the full verified fields).
Swap the host for `https://dev.idto.ai` under `DEVELOPMENT`. `404` = not found,
`410` = expired, `401` / `403` = auth error.

**Signed webhook (authoritative):** when a session finishes, IDto `POST`s the verdict and a
progress snapshot to your configured webhook URL, independent of whether the app is open.
The URL and signing secret are a per-client onboarding setting. Verify the
`X-Idto-Signature` header against the raw request bytes, dedupe on `X-Idto-Event-Id`, and
gate downstream actions on `outcome == "verified"`; pull the full PII via the state API.

## DigiLocker

The Aadhaar module uses DigiLocker OAuth. The web SDK opens DigiLocker in a **new window**
(`window.open`); the native host keeps that flow **inside** a nested `WebView` and returns
to the session — no system-browser hop, so the OAuth callback resolves in-app. The
DigiLocker hosts (`digilocker.idto.ai`, `*.digilocker.gov.in`, `*.meripehchaan.gov.in`) are
allow-listed by default, and pressing back while the popup shows dismisses the popup first,
not the whole session.

## Security notes

- **Never ship `client_secret` in the app.** Issue the short-lived `clientToken` from your
  backend. The example app injects its sandbox secret at build time from a **git-ignored**
  `demo.properties` (or `IDTO_DEMO_*` env vars) **only** because it talks to a throwaway dev
  sandbox and the `example` module is not published — nothing secret is committed.
- **Navigation is allow-listed.** The `WebView` only navigates to the `idto.ai` domain
  family, the CDN bucket, and DigiLocker by default. Add your own hosts with
  `allowedHosts` if a custom `baseUrl` or redirect needs them.
- **Same-origin, no CSP/CORS config.** The `WebView` runs under the IDto API origin
  (following `baseUrl` when set), so there is no CORS exception and no
  `Content-Security-Policy` to maintain (unlike the web integration). Config is baked into
  the shell HTML with every `<` escaped, so a hostile config value cannot break out.
- **Confirm server-side** before acting on a `completed` result, and gate on
  `outcome == "verified"`.

## Example app

The `example` module is a one-tap runnable app. It hosts an `IDtoLandingView`, fetches a
`clientToken` from the IDto **CredResolve dev sandbox** (`SdkTokenClient` calls
`POST /auth/sdk/token`), opens a real verification session, and toasts every callback.
Build and run it on the `idto_pixel` emulator or a device:

```
./gradlew :example:installDebug
```

The sandbox `client_id` / `client_secret` / `workflow_template_id` are injected at build
time from a **git-ignored** `demo.properties` (or `IDTO_DEMO_*` env vars / Gradle
properties) and are **test-only** — never ship a `client_secret` in a real app. Setup lives
in [`example/README.md`](example/README.md); if the trio is blank the app toasts a hint and
the `SmokeTest` skips.

## Versioning & web-SDK parity

This SDK is versioned independently of the web bundle. The native layer owns only the
config surface, callback bridge, and `WebView` host; **all KYC logic lives in the web SDK
CDN bundle**, selected by `env`, so new web modules and fixes reach users without a library
upgrade. A parity test pins the config surface against the web SDK's public type
definitions so the two never drift. See [`CHANGELOG.md`](CHANGELOG.md) for release history
and [`PUBLISHING.md`](PUBLISHING.md) for the release flow.

## Troubleshooting

| Symptom | Cause / fix |
| ------- | ----------- |
| **Blank `WebView` on open** | The CDN bundle failed to load. Check connectivity; `onError` fires with `step = "init"`, `error = "network_error"`. Raise `readyTimeoutMs` on slow networks. |
| **`onError` `session_expired`** | `clientToken` is stale. Reopen with a fresh token, or pass an `IDtoTokenProvider` for transparent refresh. |
| **`onError` `insufficient_credits`** | Account balance hit zero. Top up; see [Credits](#credits). |
| **`onError` `step_already_complete`** | The step succeeded in a previous session. Resume with `sessionToken` instead of restarting. |
| **`onError` `session_active`** | A session is already open. Wait for `onClose` before calling `IDto.open` again. |
| **Camera step won't open / black screen** | Use a real device (most emulators have no camera). The SDK requests the permission for you; if permanently denied, accept the in-flow **Open Settings** dialog. |
| **Navigation blocked / step stuck** | The target host isn't on the allow-list. Add it via `allowedHosts`. Enable `debug` to log blocked-navigation notices to Logcat. |
| **DigiLocker slow / times out** | DigiLocker load is the bottleneck. The flow falls back per `aadhaarConfig` `digilockerMaxFailures`; advise the user to retry. |
| **CORS / API errors with a custom `baseUrl`** | The `WebView` origin follows `baseUrl`, so calls are same-origin — confirm `baseUrl` is the actual API host. |

Enable `debug` on the config to forward in-`WebView` `console` output to Logcat while
diagnosing.

## License

[MIT](LICENSE) © IDto.
