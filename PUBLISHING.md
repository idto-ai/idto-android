# Publishing the IDto Android SDK

The library ships as an Android AAR under the coordinates
`ai.idto:idto-android:0.1.0`. It is consumed two ways: **JitPack** (builds from a git tag,
served as `com.github.idto-ai:idto-android`) and a **Maven repository** you control
(served as `ai.idto:idto-android`). Both resolve to the same artifact.

## Single source of truth

The version and coordinates live in **`sdk/build.gradle.kts`** (the `maven-publish` block):

- `group = "ai.idto"`
- `artifactId = "idto-android"`
- `version = "0.1.0"`

Change the version in exactly one place — `sdk/build.gradle.kts` — and nowhere else in
code.

## Release flow

1. **Bump the version** in `sdk/build.gradle.kts` (`version = "X.Y.Z"`).
2. **Update `CHANGELOG.md`** — move the pending items under a new `## [X.Y.Z]` heading with
   the release date (Keep a Changelog format).
3. **Commit** the version bump and changelog together.
4. **Tag** the release and push the tag:

   ```
   git tag vX.Y.Z
   git push origin vX.Y.Z
   ```

5. **JitPack** picks up the tag automatically. The first request to
   `https://jitpack.io/com/github/idto-ai/idto-android/X.Y.Z/` triggers the build
   (driven by `jitpack.yml`, which pins JDK 17). No manual upload is needed.
6. **Maven repository (optional):** publish the `ai.idto` coordinates to your controlled
   repository — see below.

## Local verification (before tagging)

Publish to the on-disk `LocalRepo` (a `file://` repo at `sdk/build/repo`) and confirm the
artifacts and coordinates:

```
./gradlew :sdk:publishReleasePublicationToLocalRepoRepository
ls sdk/build/repo/ai/idto/idto-android/0.1.0
```

The directory must contain, with the exact coordinates:

- `idto-android-0.1.0.aar` — the library
- `idto-android-0.1.0-sources.jar` — the sources jar
- `idto-android-0.1.0.pom` — POM with `ai.idto` / `idto-android` / `0.1.0`, the MIT
  license block, and SCM metadata

### Consumption check

Point a consuming project's `dependencyResolutionManagement` at the local repo and depend
on the published coordinates to prove they resolve end-to-end:

```kotlin
// settings.gradle.kts of the consumer (temporary, for verification only)
dependencyResolutionManagement {
    repositories {
        maven { url = uri("<path-to>/android_sdk/sdk/build/repo") }
        google()
        mavenCentral()
    }
}
```

```kotlin
// consumer app/build.gradle.kts
dependencies {
    implementation("ai.idto:idto-android:0.1.0")
}
```

Revert this settings-level substitution after verifying — it is not a permanent change.

## Publishing the `ai.idto` coordinates

`sdk/build.gradle.kts` wires a second Maven repository (GitHub Packages by default) that
is inert until credentials are supplied via environment variables. To publish for real:

```
export GITHUB_ACTOR=<user>
export GITHUB_TOKEN=<token-with-write:packages>
./gradlew :sdk:publishReleasePublicationToGitHubPackagesRepository
```

For Maven Central (Sonatype), add the OSSRH repository and signing to the publishing block
and run the matching `publish…Repository` task. The POM already carries the name,
description, project URL, MIT license, developer, and SCM entries Central requires.

## Checklist

- [ ] `version` bumped in `sdk/build.gradle.kts`
- [ ] `CHANGELOG.md` updated with the new version and date
- [ ] Local publish verified (`sdk/build/repo/ai/idto/idto-android/<version>/` has AAR +
      sources jar + POM)
- [ ] Consumption check resolves `ai.idto:idto-android:<version>`
- [ ] `git tag vX.Y.Z` pushed (JitPack build)
- [ ] `ai.idto` coordinates published to the controlled Maven repository (if applicable)
