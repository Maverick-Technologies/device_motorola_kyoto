#!/usr/bin/env bash
# Experimental Android 15 UI features that EvoX 10 ships switched off (read-only aconfig flags,
# constant-folded, so they cannot be turned on on a running build). Each feature folder holds
# bp1a flag values; this copies them into build/release/aconfig/bp1a/<package>/ as
# kyoto_<flag>_flag_values.textproto, which the existing Android.bp there already globs.
#
#   compose-qs       Compose Quick Settings with resizable tiles in edit mode
#                    (qs_ui_refactor + qs_ui_refactor_compose_fragment)
#   volume-redesign  new volume dialog (systemui/volume/dialog) instead of VolumeDialogImpl;
#                    EvoX's own volume dialog options do not apply to it
#
# Values are READ_WRITE, so a feature can be switched off on the phone without a rebuild:
#   adb shell su -c 'aflags disable com.android.systemui.volume_redesign' && adb reboot
#
# Usage from the source root (each feature defaults to on):
#   COMPOSE_QS=off VOLUME_REDESIGN=on bash device/motorola/kyoto/experiments/flags/apply.sh
set -euo pipefail
HERE=device/motorola/kyoto/experiments/flags
BP1A=build/release/aconfig/bp1a

[[ -d $BP1A ]] || { echo "Run from the source root ($BP1A missing)"; exit 1; }

# Start clean so switching a feature off removes its values
find "$BP1A" -name 'kyoto_*_flag_values.textproto' -delete

enabled() {
  case $1 in
    compose-qs) [[ ${COMPOSE_QS:-on} != off ]] ;;
    volume-redesign) [[ ${VOLUME_REDESIGN:-on} != off ]] ;;
    *) return 1 ;;
  esac
}

for feature_dir in "$HERE"/*/; do
  feature=$(basename "$feature_dir")
  if ! enabled "$feature"; then
    echo "  off  $feature"
    continue
  fi
  for package_dir in "$feature_dir"*/; do
    package=$(basename "$package_dir")
    dest=$BP1A/$package
    if [[ ! -f $dest/Android.bp ]]; then
      echo "  FAIL $feature: $dest/Android.bp missing (package not in bp1a release config)"
      exit 1
    fi
    for value in "$package_dir"*_flag_values.textproto; do
      flag=$(sed -n 's/^ *name: *"\(.*\)"/\1/p' "$value")
      # An upstream value for the same flag would be a duplicate definition
      if grep -lE "^ *name: *\"$flag\"" "$dest"/*_flag_values.textproto 2>/dev/null \
          | grep -qv '/kyoto_'; then
        echo "  FAIL $feature: $package.$flag is already set upstream in $dest"
        exit 1
      fi
      cp "$value" "$dest/kyoto_$(basename "$value")"
    done
  done
  echo "  on   $feature"
done
