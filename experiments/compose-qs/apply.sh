#!/usr/bin/env bash
# Experiment: Compose Quick Settings (Pixel-style panel with resizable tiles) on EvoX 10.
#
# EvoX 10.7 ships com.android.systemui.qs_ui_refactor_compose_fragment off (read-only,
# constant-folded), so the Compose QS panel and its edit-mode tile resizing are compiled out.
# This adds a bp1a flag value that turns it on, marked READ_WRITE so it can be switched off
# on the phone without a rebuild:
#   adb shell su -c 'aflags disable com.android.systemui.qs_ui_refactor_compose_fragment' && adb reboot
#
# Usage from the source root:  bash device/motorola/kyoto/experiments/compose-qs/apply.sh [on|off]
set -euo pipefail
MODE=${1:-on}
HERE=device/motorola/kyoto/experiments/compose-qs
DEST=build/release/aconfig/bp1a/com.android.systemui

[[ -d build/release/aconfig/bp1a ]] || { echo "Run from the source root (build/release/aconfig/bp1a missing)"; exit 1; }

if [[ $MODE == off ]]; then
  rm -rf "$DEST"
  echo "Compose QS experiment: off (SystemUI flag defaults)"
  exit 0
fi

if [[ -d $DEST && ! -f $DEST/.kyoto-compose-qs ]]; then
  echo "$DEST already exists and was not created by this script; not touching it."
  exit 1
fi
mkdir -p "$DEST"
cp "$HERE/com.android.systemui/Android.bp.in" "$DEST/Android.bp"
cp "$HERE"/com.android.systemui/*_flag_values.textproto "$DEST/"
touch "$DEST/.kyoto-compose-qs"
echo "Compose QS experiment: on ($DEST)"
