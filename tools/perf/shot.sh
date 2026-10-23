#!/bin/bash
# A pixelated screenshot (30x65 blocks): enough to see which screen is up and where the dock is,
# never the page's content. The full-size capture is deleted right away. Usage: shot.sh <out.png>
source "$(dirname "$0")/env.sh"
[ -n "$1" ] || { echo "usage: shot.sh <out.png>"; exit 1; }
# Phones with a second screen need the main display's id; the first HWC display is the main one.
ID=$(adb shell dumpsys SurfaceFlinger --display-id | grep -m1 'HWC display 0' | sed -E 's/^Display ([0-9]+).*/\1/')
TMP=$(mktemp --suffix=.png)
adb shell screencap ${ID:+-d $ID} -p /sdcard/koan-shot.png
adb pull /sdcard/koan-shot.png "$TMP" >/dev/null; adb shell rm /sdcard/koan-shot.png
magick "$TMP" -scale 30x65 -scale 302x656 "$1"; rm "$TMP"
