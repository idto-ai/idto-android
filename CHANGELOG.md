# Changelog

All notable changes to the IDto Android SDK are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] — 2026-07-15

Initial release. A pure-Kotlin, Java-first Android library that hosts the IDto web SDK
CDN bundle (`idto.js`) in a `WebView` and exposes a typed native surface. Available on
JitPack as `com.github.idto-ai:idto-android:0.1.0` (see [Installation](README.md#installation)).

### Added

- **`IDto.open` / `IDto.close` / `IDto.isOpen`** — single entry point that starts the
  verification flow from any `Context` (Kotlin, Java, Compose, or Views).
- **`IDtoConfig.Builder`** — fluent config for tokens, session, branding, display mode,
  environment, and per-module options (`aadhaarConfig`, `faceMatchConfig`, `panConfig`,
  `nameMatchConfig`, `colors`, `bottomSheet`).
- **Typed callbacks** — `IDtoEventListener` with `onStepComplete`, `onWorkflowComplete`
  (non-terminal), `onAbandon`, `onError`, and `onClose` (terminal, fires once), delivered
  on the main thread with typed payload classes.
- **SDK-owned camera & microphone permissions** — the library declares `CAMERA` /
  `RECORD_AUDIO` and requests them at runtime before the `WebView` opens, including a
  permanent-denial Settings recovery flow. No host permission code required.
- **Presentation** — `FULL_SCREEN` and native `BOTTOM_SHEET` (fixed 90% height, dim
  backdrop, hand-rolled animations) via a translucent host Activity that never recreates
  the `WebView`.
- **`IDtoLandingView`** — a batteries-included landing screen (logo header, hero, numbered
  steps, CTA, centered footer) that owns the token lifecycle and opens the sheet. Ships
  DriveX-style default copy — a `businessName`-interpolated subtitle and three steps
  (Confirm your details / Capture your documents / Instant result); the hero note and
  trust row are blank by default and appear only when overridden via `copy`.
- **`example` module** — a one-tap runnable app (DriveX-branded landing) whose demo
  credentials are externalized to `IDTO_DEMO_*` env vars / Gradle properties / a
  git-ignored `demo.properties` (template `demo.properties.example`); nothing secret is
  committed.
- **Token auto-refresh** — `IDtoTokenProvider` / `IDtoTokenCallback` swap in a fresh
  `clientToken` mid-session over the shell bridge.
- **DigiLocker OAuth** — the `window.open` flow completes in-app in a nested `WebView`.
- **Native report download** — captures the result PDF and shares it via a library-owned
  `FileProvider`, then deletes the staged file. No optional dependencies.
- **Hardened host** — navigation allow-list (extensible via `allowedHosts`), config-injection
  escaping, configurable `readyTimeoutMs`, and deterministic init-failure taxonomy.

### Parity

The native layer owns only the config surface, callback bridge, and `WebView` host — all
KYC logic lives in the web SDK CDN bundle, selected by `env`. A parity test pins the config
surface against the web SDK's public type definitions so the two never drift; new web SDK
modules and fixes reach users without a library upgrade.

[0.1.0]: https://github.com/idto-ai/idto-android/releases/tag/0.1.0
