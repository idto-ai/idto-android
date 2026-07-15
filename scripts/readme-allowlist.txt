# README docs-accuracy allowlist.
#
# Every backtick-quoted token in README.md must exist verbatim in sdk/src/main (or
# sdk/build.gradle.kts). Symbols the gate cannot see there are allowlisted below. The
# canonical sweep strips only full-line `#` comments, so each allowlisted symbol sits
# ALONE on its own line, with its justification on the preceding `#` comment line(s).

# --- Published Maven coordinates (proven by the Phase-6 publishing gate, not source) ---
# canonical Maven coordinate; lives in sdk/build.gradle.kts as of Phase 6b
ai.idto:idto-android:0.1.0
# canonical coordinate without the version, used in prose
ai.idto:idto-android
# JitPack coordinate served from the git tag
com.github.idto-ai:idto-android:0.1.0
# JitPack coordinate without the version, used in prose
com.github.idto-ai:idto-android
# artifactId / rootProject.name (settings.gradle.kts, not scanned by the gate)
idto-android

# --- Host-side Android / Compose / Gradle tokens (host app owns these, not the SDK) ---
# Jetpack Compose interop wrapper used to host IDtoLandingView
AndroidView
# AndroidX base Activity a Compose host runs in
ComponentActivity
# android.util.Log, used in a host-side snippet
Log
# HTTP Authorization scheme in the state-API example
Bearer
# HTTP method in the token / state-API examples
POST
# Gradle publishing plugin, added to sdk/build.gradle.kts in Phase 6b
maven-publish
# jitpack.yml JDK selector
openjdk17
# Gradle settings file (Groovy DSL)
settings.gradle
# Gradle settings file (Kotlin DSL)
settings.gradle.kts

# --- Public IDto API dotted forms (members exist in source as close()/isOpen(); the
#     dotted call string itself is not written in sdk/src/main) ---
# IDto.close() member of object IDto
IDto.close
# IDto.isOpen() member of object IDto
IDto.isOpen
# IDtoLandingConfig.Builder nested class (source declares `class Builder` inside it)
IDtoLandingConfig.Builder

# --- example module symbols (gate scans sdk/src/main only, not example/src) ---
# example Gradle module name
example
# example token client that calls POST /auth/sdk/token
SdkTokenClient
# example test-only credential holder
DevCredentials
# emulator AVD name used to run the example
idto_pixel
# git-ignored file that feeds the example's demo credentials at build time
demo.properties
# checked-in template the example's demo.properties is copied from
demo.properties.example
# env-var prefix for the example's demo credentials
IDTO_DEMO_*
# example setup doc referenced from the root README
example/README.md
# instrumented smoke test in the example module
SmokeTest
# backend wire key for the workflow template id
workflow_template_id

# --- Backend / integration tokens (not SDK symbols) ---
# backend credential exchanged for a clientToken
client_id
# backend secret, never shipped in the app
client_secret
# your backend's IDto API secret (env var)
IDTO_API_SECRET
# web header the web integration must set (contrasted as NOT needed here)
Content-Security-Policy
# Flutter permission package named when contrasting the Flutter SDK
permission_handler

# --- Web-SDK wire strings emitted verbatim by the CDN bundle (not native symbols) ---
# outcome verdict string from the web SDK
verified
# outcome verdict string from the web SDK
not_verified
# session verdict field name from the web SDK / state API
outcome
# onError string surfaced by the web SDK
workflow_not_found
# onError string surfaced by the web SDK
step_already_complete
# onError string surfaced by the web SDK
step_completion_failed
# onError string surfaced by the web SDK
session_expired
# onError string surfaced by the web SDK
timeout
# session status string from the state API
in_progress
# session status string from the state API
completed
# session status string from the state API
expired
# session status string from the state API
abandoned
# session status the web SDK deliberately does NOT emit (documented absence)
failed
# continuity-mode name configured server-side
merchant_injects
# name-match result field (per-pair match boolean)
match
# name-match result field (per-pair score)
score
# name-match result field (top-level passed)
passed
# module slug (server-side / web SDK)
name_match
# module slug (server-side / web SDK)
mobile_verification
# module slug (server-side / web SDK)
aadhar_verification
# module slug (server-side / web SDK)
pan_verification
# module slug (server-side / web SDK)
face_match
# module slug (server-side / web SDK)
account_verification
# module slug (server-side / web SDK)
driving_licence
# module slug (server-side / web SDK)
vehicle_rc
# module slug (server-side / web SDK)
e_sign
# module slug (server-side / web SDK)
govt_id_selection
# web SDK global object name (referenced in the error taxonomy)
IDtoSDK
# web SDK CDN bundle filename
idto.js
# CDN bucket path segment
idto-sdk-bucket
# IDto domain family
idto.ai
# DigiLocker allow-list host (string literal in NavigationPolicy)
digilocker.idto.ai
# DigiLocker allow-list domain (string literal in NavigationPolicy)
digilocker.gov.in
# DigiLocker allow-list domain (string literal in NavigationPolicy)
meripehchaan.gov.in
# webhook header emitted by the IDto backend
X-Idto-Signature
# webhook header emitted by the IDto backend
X-Idto-Event-Id
# doc filename referenced from the README
CHANGELOG.md
# doc filename referenced from the README
PUBLISHING.md
