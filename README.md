# YAML JSON-Schema `$ref` Navigation (IntelliJ plugin)

Adds Ctrl/Cmd-click **Go to Declaration** for `$ref` values inside JSON-Schema files
written in **YAML** — the feature IntelliJ ships for `.json` but not `.yaml`
(JetBrains [IJPL-166197](https://youtrack.jetbrains.com/issue/IJPL-166197), open).

Resolves:
- in-file pointers: `#/$defs/Foo` (incl. nested `#/$defs/A/B`)
- sibling / self files: `data.yaml#/$defs/Foo`
- relative paths: `../sticky_type/data.yaml#/$defs/StickyType`
- whole-file refs with no fragment: `../shared_models.yaml`

## Install

Grab the latest `yaml-ref-nav-*.zip` from the [Releases](../../releases) page, then in your
IDE: **Settings ▸ Plugins ▸ ⚙ ▸ Install Plugin from Disk…**, pick the zip, and restart.

To build it yourself instead, see below.

## Build

> **Gradle JVM must be 21**, not your default JDK 25 (Gradle 8.10 doesn't run on 25).
> In IDEA: Settings ▸ Build Tools ▸ Gradle ▸ *Gradle JVM* → pick the JBR 21 / OpenJDK 21
> you already have installed.

Easiest — in IntelliJ IDEA (with the Gradle + Plugin DevKit plugins):

1. **File ▸ Open** this folder. Let Gradle sync.
2. Run the Gradle task **`buildPlugin`** (Gradle tool window ▸ Tasks ▸ intellij platform).
3. Install the zip it produces in `build/distributions/yaml-ref-nav-0.1.0.zip` via
   **Settings ▸ Plugins ▸ ⚙ ▸ Install Plugin from Disk…**, then restart.

Or try it live without installing: run the **`runIde`** task — it launches a sandbox
IDE with the plugin loaded.

From the command line — the Gradle wrapper is checked in, so no local Gradle install is
needed:

```bash
./gradlew buildPlugin     # -> build/distributions/yaml-ref-nav-0.1.0.zip
./gradlew runIde          # or launch a sandbox IDE
```

## Matching your IDE

`build.gradle.kts` builds against `intellijIdeaCommunity("2024.2.5")`. The plugin only
needs the bundled YAML plugin, so it runs on IDEA Ultimate, PyCharm, etc. If loading
fails on a newer IDE, set the version string to your exact build (e.g. `"2026.1"`).

## Releasing

CI (`.github/workflows/ci.yml`) runs `./gradlew build` on every push and PR to `main`.

Releases are cut by pushing a `v*` tag. The release workflow
(`.github/workflows/release.yml`) builds the plugin on JDK 21 and publishes a GitHub
Release with the distribution zip attached:

```bash
# bump `version` in build.gradle.kts to match, then:
git tag v0.1.0
git push origin main --tags
```

## License

[Apache License 2.0](LICENSE).
