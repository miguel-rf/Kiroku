#!/bin/sh

set -eu

width_px="$(adb shell wm size | sed -n 's/.*size: \([0-9][0-9]*\)x.*/\1/p' | tail -n 1)"
density="$(adb shell wm density | sed -n 's/.*density: \([0-9][0-9]*\).*/\1/p' | tail -n 1)"

if [ -z "${width_px}" ] || [ -z "${density}" ] || [ "${density}" -eq 0 ]; then
  echo "Unable to determine emulator width and density" >&2
  exit 1
fi

width_dp=$((width_px * 160 / density))
echo "Emulator display: ${width_px}px at ${density}dpi = ${width_dp}dp wide"

if [ "${width_dp}" -lt "${MIN_WIDTH_DP}" ] || [ "${width_dp}" -gt "${MAX_WIDTH_DP}" ]; then
  echo "Unexpected ${FORM_FACTOR} width: ${width_dp}dp" >&2
  exit 1
fi

exec ./gradlew connectedDebugAndroidTest --no-daemon --console=plain
