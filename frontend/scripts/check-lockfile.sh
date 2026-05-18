#!/usr/bin/env bash
# Fail if pnpm-lock.yaml references any source outside the npm registry.
#
# pnpm-lock.yaml v9 stores only an integrity hash for registry packages —
# tarball/git/github/file/link/workspace URLs only appear when a dependency
# was deliberately resolved from a non-registry source. Spotting any of
# those is enough to catch a lockfile-injection PR that swaps a real
# dependency for attacker-hosted code.
#
# Run before `pnpm install` so a poisoned lockfile cannot execute code
# (via fetch, postinstall, or otherwise) before the gate fires.
#
# Requires `yq` (mikefarah/yq v4+). Preinstalled on GitHub Actions
# ubuntu-latest runners.

set -euo pipefail

LOCK_FILE="pnpm-lock.yaml"

if [ ! -f "$LOCK_FILE" ]; then
  echo "ERROR: $LOCK_FILE not found" >&2
  exit 2
fi

# Check 1: every packages.*.resolution must contain only the "integrity"
# key. Any other key (tarball, repo, directory, type, commit, url, path)
# means the resolution points at a non-registry source.
bad_resolutions=$(yq '
  [.packages // {} | to_entries[]
   | select(.value.resolution | keys - ["integrity"] | length > 0)
   | .key]
' "$LOCK_FILE")

# Check 2: every importer-level specifier/version must be a plain semver
# range, not a non-registry protocol.
BAD_PROTOCOL_REGEX='^(git\+|git://|github:|file:|link:|workspace:|https?://)'

bad_specs=$(REGEX="$BAD_PROTOCOL_REGEX" yq '
  [.importers // {} | .. | (.specifier? // "") | select(test(strenv(REGEX)))]
  + [.importers // {} | .. | (.version? // "") | select(test(strenv(REGEX)))]
' "$LOCK_FILE")

failed=0

if [ "$bad_resolutions" != "[]" ]; then
  echo "ERROR: non-registry resolutions in $LOCK_FILE:" >&2
  echo "$bad_resolutions" >&2
  failed=1
fi

if [ "$bad_specs" != "[]" ]; then
  echo "ERROR: non-registry importer specifier/version in $LOCK_FILE:" >&2
  echo "$bad_specs" >&2
  failed=1
fi

if [ "$failed" -ne 0 ]; then
  echo "" >&2
  echo "If a non-registry source is intentional, update scripts/check-lockfile.sh to allow it." >&2
  exit 1
fi

echo "Lockfile check passed: only npm-registry sources in $LOCK_FILE."
