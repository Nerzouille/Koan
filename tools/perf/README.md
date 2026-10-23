# Perf scripts

A/B checks of two Koan builds on a real phone over adb: frame timing of Koan's own UI (gfxinfo)
and start times. Page rendering (GeckoView) is not measured: it is the same engine in both builds.

| Script | What it does |
|---|---|
| `frames.sh <label> [rounds]` | Page ↔ overview, stack scroll + flings, space switch on the dock, drawer open/close, dock hide/show (page scrolled down and up); one gfxinfo dump per scenario and round |
| `start.sh <label> [rounds]` | Cold start (force-stop, launch) and warm start (from Home), `am start -W` TotalTime |
| `compare.py <label> ...` | Median janky %, p50/90/95/99 frame times and start times per label |
| `shot.sh <out.png>` | Pixelated screenshot to check which screen is up, without seeing page content |
| `env.sh` | Shared settings: coordinates, output folder, the "Koan in front" guard, `KOAN_PKG` (e.g. the `.staging` copy) |

Results go to `app/build/perf/<label>/` (gitignored; `KOAN_PERF_OUT` overrides it). Never commit
them: they come from the owner's phone.

## Compare two builds

1. Install build A. Open Koan on an ordinary page (not the start page, nothing typed), then leave
   the phone alone. Check with `./shot.sh /tmp/a.png` that the dock is where `env.sh` expects it.
2. `./frames.sh a 5`, then `./start.sh a 5`.
3. Install build B **over** A (`adb install -r`, same signing key, versionCode not lower) so it
   runs on the same tabs and data. Do one throwaway round first (`./frames.sh warmup 1`): caches
   and the page are cold after an install.
4. `./frames.sh b 5`, then `./start.sh b 5`.
5. `./compare.py a b`.

A downgrade needs an uninstall (data wiped), so plan the order: older versionCode first. When the
real app is already newer (or an older build can't open its database), compare two **staging**
copies instead (`assembleStaging`, `KOAN_PKG=dev.nerzouille.koan.staging`): same fresh data for
both, the real app untouched. Start the dock scenario on a long page.

## Rules for fair numbers

- Same build type for both: an R8 **release** (or the published APK), never debug or dev.
- Check `adb shell pm art dump dev.nerzouille.koan | grep arm64` shows the same compile status
  (`speed-profile`) for both; force it with `adb shell cmd package compile -m speed-profile -f dev.nerzouille.koan` if not.
- Thermal status 0 at the start and end of each run (`frames.sh` logs it). Long adb runs while
  charging heat the phone and invalidate the numbers.
- scrcpy mirroring costs CPU. Prefer no mirror; if one is open, it must be open for both builds.
- Differences under ~1 % janky or a few ms of p90 are run-to-run noise.

## Safety

The phone is a daily phone with real tabs. Every gesture first checks that Koan has the focus and
aborts otherwise; still, look at a `shot.sh` capture before starting. Only vertical drags in the
card area (a horizontal swipe there closes a card), never Back (it closes the tab on its first
page), never typing.
