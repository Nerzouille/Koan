# Shared settings for the perf scripts (sourced, not run).
# Tap/swipe coordinates are for a 1208x2624 screen at 480 dpi. On another phone, find the dock
# with shot.sh and override them in the environment (e.g. DOCK_Y=2306 on a 1080x2460 screen).

P=dev.nerzouille.koan
HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# Results stay out of git (CLAUDE.md: measurements are never committed). `gradlew clean` wipes them.
OUT=${KOAN_PERF_OUT:-$HERE/../../app/build/perf}

CX=${CX:-604}             # screen centre x
DOCK_Y=${DOCK_Y:-2445}    # dock row
DOCK_DOWN_Y=${DOCK_DOWN_Y:-2620}
STACK_TOP=${STACK_TOP:-900}
STACK_BOTTOM=${STACK_BOTTOM:-1900}
EDGE_L=${EDGE_L:-200}
EDGE_R=${EDGE_R:-1000}

# Stops everything unless Koan has the focus: never send input to another app.
guard() {
  adb shell dumpsys window | grep mCurrentFocus | grep -q "$P" || { echo "ABORT: Koan not in front"; exit 1; }
}
sw() { guard; adb shell input swipe "$@"; }
therm() { adb shell dumpsys thermalservice | grep -m1 'Thermal Status'; }
