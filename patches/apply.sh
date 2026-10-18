#!/usr/bin/env bash
# Apply kyoto source patches to the synced Evolution X tree.
#
# patches/<project path>/*.patch is applied with `git am` to <project path>
# (e.g. patches/frameworks/base/0001-*.patch -> frameworks/base). A patch that is already
# applied is skipped, so this is safe to re-run; `repo sync` drops the commits again.
#
# Usage from the source root:  bash device/motorola/kyoto/patches/apply.sh
set -euo pipefail
HERE=device/motorola/kyoto/patches

[[ -d $HERE ]] || { echo "Run from the source root ($HERE missing)"; exit 1; }

failed=0
while IFS= read -r patch; do
  rel=${patch#"$HERE"/}
  project=${rel%/*}
  name=${rel##*/}
  if [[ ! -d $project/.git && ! -f $project/.git ]]; then
    echo "  skip  $name: $project is not a git project"; failed=1; continue
  fi
  if git -C "$project" apply --reverse --check "$PWD/$patch" 2>/dev/null; then
    echo "  done  $project: $name (already applied)"
  elif git -C "$project" apply --check "$PWD/$patch" 2>/dev/null; then
    git -C "$project" -c user.name="kyoto builder" -c user.email=builder@localhost \
      am -q --keep-cr "$PWD/$patch"
    echo "  apply $project: $name"
  else
    echo "  FAIL  $project: $name does not apply (upstream changed?)"; failed=1
  fi
done < <(find "$HERE" -mindepth 2 -name '*.patch' | sort)

exit $failed
