#!/usr/bin/env bash
# Checks, on a connected phone, that GeckoView still fails Koan's offline stand-in address the way
# Koan relies on (engine/ErrorPages.kt, StandIns in engine/ErrorRules.kt): an instant
# ERROR_CONNECTION_REFUSED. If an engine update blocks port 4, upgrades loopback to https, or
# treats it as a local-network request, Koan abandons the stand-in at run time (the page stays and
# a toast says offline), so offline pages behind a VPN would show late again: this check fails.
# Run it after every GeckoView / android-components bump (docs/ARCHITECTURE.md).
#
# Usage: tools/check-stand-in.sh [app id]   (default dev.nerzouille.koan)
# Needs a dev, debug or staging build (they log `KoanError`). The address it opens carries no
# valid token, so Koan shows it as itself: "This site refused the connection" in a new tab, which
# also checks that a site's copy of the stand-in is never shown as another address. Close that tab
# afterwards.
set -euo pipefail

APP="${1:-dev.nerzouille.koan}"
URL='http://127.0.0.1:4/koan-offline?t=check&u=https%3A%2F%2Fexample.com%2F'

adb shell pm path "$APP" >/dev/null || { echo "FAIL: $APP isn't installed"; exit 1; }
adb logcat -c
adb shell am start -a android.intent.action.VIEW -d "'$URL'" "$APP" >/dev/null 2>&1

line=""
for _ in $(seq 1 40); do
    line=$(adb logcat -d -s KoanError | grep ' error ' | tail -1 || true)
    [ -n "$line" ] && break
    sleep 0.5
done

if [ -z "$line" ]; then
    echo "FAIL: no load error logged in 20 s (a dev, debug or staging build? the page may have loaded)"
    exit 1
fi
case "$line" in
    *"error ERROR_CONNECTION_REFUSED "*"kind=Refused"*)
        echo "OK: the stand-in fails with ERROR_CONNECTION_REFUSED, and a site's copy shows as itself"
        echo "    (close the tab it opened)"
        ;;
    *)
        echo "FAIL: the stand-in now fails differently:"
        echo "    $line"
        echo "    Pick another stand-in (StandIns in engine/ErrorRules.kt) before releasing."
        exit 1
        ;;
esac
