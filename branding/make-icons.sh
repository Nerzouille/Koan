#!/usr/bin/env bash
# Builds the app's launcher icons from koan-icon.png (needs ImageMagick 7). Run from anywhere:
#   branding/make-icons.sh
#
# Adaptive icon: the whole artwork is the visible 72dp of the 108dp layer, so no mask cuts the K.
# The 18dp bleed around it (only seen while a launcher moves the layers) is the artwork's own edge
# pixels stretched outwards and softened. The themed icon uses res/drawable/ic_launcher_monochrome.
# minSdk 30: no legacy (pre-adaptive) bitmaps; res/mipmap-anydpi covers every density.
set -euo pipefail
cd "$(dirname "$0")"
SRC=koan-icon.png
RES=../app/src/main/res
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

size=$(magick identify -format %w "$SRC")
bleed=$((size / 4))          # 18dp of 72dp on each side
canvas=$((size + 2 * bleed))

# Edge-extended and blurred, with the sharp artwork on top: no seam where the two meet.
magick "$SRC" -set option:distort:viewport "${canvas}x${canvas}-${bleed}-${bleed}" \
    -virtual-pixel Edge -distort SRT 0 +repage -blur 0x$((size / 30)) \
    "$SRC" -geometry "+${bleed}+${bleed}" -composite "$TMP/layer.png"

for d in mdpi:1 hdpi:1.5 xhdpi:2 xxhdpi:3 xxxhdpi:4; do
    name=${d%%:*}
    scale=${d##*:}
    dir="$RES/mipmap-$name"
    mkdir -p "$dir"
    layer=$(awk "BEGIN { print int(108 * $scale) }")
    magick "$TMP/layer.png" -filter Lanczos -resize "${layer}x${layer}" -quality 92 "$dir/ic_launcher_background.webp"
done

# In-app copy (About, first-run card): the artwork alone, the UI rounds its corners.
mkdir -p "$RES/drawable-nodpi"
magick "$SRC" -filter Lanczos -resize 432x432 -quality 92 "$RES/drawable-nodpi/koan_icon.webp"
