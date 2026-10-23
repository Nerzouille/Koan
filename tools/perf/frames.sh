#!/bin/bash
# Frame timing of Koan's own UI (gfxinfo) over five scenarios. Usage: frames.sh <label> [rounds=5]
# Start on the page view of a tab with a long page; it ends there too. Only safe gestures: vertical drags
# on the stack, horizontal swipes on the dock (a horizontal swipe in the card area closes a card).
source "$(dirname "$0")/env.sh"
L=$1; N=${2:-5}; D=$OUT/$L; mkdir -p "$D"
[ -n "$L" ] || { echo "usage: frames.sh <label> [rounds]"; exit 1; }
reset() { adb shell dumpsys gfxinfo $P reset >/dev/null; }
grab() { adb shell dumpsys gfxinfo $P > "$D/$1.txt"; }

adb shell dumpsys package $P | grep -m1 versionName | tee -a "$D/log"
echo "$L start $(therm)" | tee -a "$D/log"
for r in $(seq 1 "$N"); do
  # 1. page <-> overview: swipe up on the dock, swipe down on the dock
  reset
  for i in 1 2 3; do
    sw $CX $DOCK_Y $CX 1300 250; sleep 1.2
    sw $CX 2300 $CX $DOCK_DOWN_Y 150; sleep 1.2
  done
  grab "open-$r"

  # into the overview for 2-4
  sw $CX $DOCK_Y $CX 1300 250; sleep 1.2

  # 2. stack scroll: slow drags, then flings
  reset
  for i in 1 2 3; do
    sw $CX $STACK_TOP $CX $STACK_BOTTOM 400; sleep 0.8
    sw $CX $STACK_BOTTOM $CX $STACK_TOP 400; sleep 0.8
  done
  sw $CX $STACK_TOP $CX 2000 120; sleep 2.5
  sw $CX 2000 $CX $STACK_TOP 120; sleep 2.5
  grab "scroll-$r"

  # 3. space switch on the dock, there and back
  reset
  for i in 1 2; do
    sw $EDGE_R $DOCK_Y $EDGE_L $DOCK_Y 250; sleep 1.2
    sw $EDGE_L $DOCK_Y $EDGE_R $DOCK_Y 250; sleep 1.2
  done
  grab "space-$r"

  # 4. drawer: swipe up on the dock (compact), swipe down to close
  reset
  for i in 1 2 3; do
    sw $CX $DOCK_Y $CX 1700 250; sleep 1.2
    sw $CX 1700 $CX 2600 250; sleep 1.2
  done
  grab "drawer-$r"

  # back to the page
  sw $CX 2300 $CX $DOCK_DOWN_Y 150; sleep 1.5

  # 5. dock hide/show: scroll the page down (the dock slides away) and back up (it comes back)
  reset
  for i in 1 2 3; do
    sw $CX 1800 $CX 900 250; sleep 1
    sw $CX 900 $CX 1800 250; sleep 1
  done
  grab "dock-$r"
  echo "round $r $(therm)" | tee -a "$D/log"
done
