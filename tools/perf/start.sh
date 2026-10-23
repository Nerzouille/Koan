#!/bin/bash
# Cold start (force-stop, then launch) and warm start (from Home) times. Usage: start.sh <label> [rounds=5]
source "$(dirname "$0")/env.sh"
L=$1; N=${2:-5}; D=$OUT/$L; mkdir -p "$D"
[ -n "$L" ] || { echo "usage: start.sh <label> [rounds]"; exit 1; }
for r in $(seq 1 "$N"); do
  adb shell am force-stop $P; sleep 2
  adb shell am start -W -n $P/.MainActivity | grep TotalTime | sed "s/^/cold $r /" | tee -a "$D/start"
  sleep 4
  adb shell input keyevent KEYCODE_HOME; sleep 2
  adb shell am start -W -n $P/.MainActivity | grep TotalTime | sed "s/^/warm $r /" | tee -a "$D/start"
  sleep 3
done
