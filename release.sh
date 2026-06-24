#!/usr/bin/env bash
#
# Cut a release: bump the version, commit, tag, and push.
# The GitHub Actions release workflow (.github/workflows/release.yml) then builds
# the plugin and publishes a GitHub Release with the distribution zip attached.
#
# Usage:
#   ./release.sh 0.2.0          # bump to 0.2.0, verify build, commit, tag v0.2.0, push
#   ./release.sh 0.2.0 --no-build   # skip the local build check
#
set -euo pipefail

BUILD_FILE="build.gradle.kts"
BRANCH="main"

die() { echo "error: $*" >&2; exit 1; }

# --- args ----------------------------------------------------------------
VERSION="${1:-}"
RUN_BUILD=1
for arg in "${@:2}"; do
    case "$arg" in
        --no-build) RUN_BUILD=0 ;;
        *) die "unknown option: $arg" ;;
    esac
done

[ -n "$VERSION" ] || die "usage: ./release.sh <version> [--no-build]   (e.g. ./release.sh 0.2.0)"
[[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || die "version must look like X.Y.Z (got '$VERSION')"

TAG="v$VERSION"

# --- preflight checks ----------------------------------------------------
cd "$(dirname "$0")"
[ -f "$BUILD_FILE" ] || die "$BUILD_FILE not found — run from the project root"

CURRENT_BRANCH="$(git rev-parse --abbrev-ref HEAD)"
[ "$CURRENT_BRANCH" = "$BRANCH" ] || die "on branch '$CURRENT_BRANCH', expected '$BRANCH'"

git diff --quiet && git diff --cached --quiet || die "working tree is dirty — commit or stash first"

git fetch --quiet --tags
git rev-parse -q --verify "refs/tags/$TAG" >/dev/null && die "tag $TAG already exists"

# Make sure local main is not behind the remote.
if git rev-parse -q --verify "origin/$BRANCH" >/dev/null; then
    BEHIND="$(git rev-list --count "HEAD..origin/$BRANCH")"
    [ "$BEHIND" -eq 0 ] || die "local $BRANCH is $BEHIND commit(s) behind origin/$BRANCH — pull first"
fi

# --- bump version --------------------------------------------------------
OLD_VERSION="$(grep -E '^version = ' "$BUILD_FILE" | sed -E 's/^version = "(.*)"/\1/')"
[ -n "$OLD_VERSION" ] || die "could not find 'version = \"...\"' in $BUILD_FILE"
[ "$OLD_VERSION" != "$VERSION" ] || die "version is already $VERSION"

echo "Bumping version: $OLD_VERSION -> $VERSION"
# Portable in-place edit (works on both macOS/BSD and GNU sed).
perl -pi -e "s/^version = \"\Q$OLD_VERSION\E\"/version = \"$VERSION\"/" "$BUILD_FILE"
grep -qE "^version = \"$VERSION\"$" "$BUILD_FILE" || die "version bump failed"

# --- verify build --------------------------------------------------------
if [ "$RUN_BUILD" -eq 1 ]; then
    echo "Verifying build (./gradlew buildPlugin)..."
    echo "  note: Gradle must run on JDK 21 — set JAVA_HOME if your default differs."
    ./gradlew buildPlugin
else
    echo "Skipping build check (--no-build)."
fi

# --- commit, tag, push ---------------------------------------------------
git add "$BUILD_FILE"
git commit -m "Release $TAG"
git tag -a "$TAG" -m "Release $TAG"

echo
echo "About to push commit and tag $TAG to origin/$BRANCH."
read -r -p "Proceed? [y/N] " reply
case "$reply" in
    [yY]*) ;;
    *) echo "Aborted. Undo locally with: git tag -d $TAG && git reset --hard HEAD~1"; exit 1 ;;
esac

git push origin "$BRANCH"
git push origin "$TAG"

echo
echo "Pushed $TAG. The release workflow will build and publish the GitHub Release:"
echo "  https://github.com/nidi3/yaml-ref-nav/actions"
echo "  https://github.com/nidi3/yaml-ref-nav/releases"
