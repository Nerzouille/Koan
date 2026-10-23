#!/usr/bin/env bash
# Builds the app's launcher icons from koan-icon.png (needs ImageMagick 7). Run from anywhere:
#   branding/make-icons.sh
#
# Adaptive icon: the whole artwork is the visible 72dp of the 108dp layer, so no mask cuts the K.
# The 18dp bleed around it (only seen while a launcher moves the layers) is the artwork's own edge
# pixels stretched outwards and softened. The themed icon uses res/drawable/ic_launcher_monochrome.
# Legacy icons (48dp, square and round) are only for launchers that ignore adaptive icons.
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

# Rounded-square and circle cut-outs of the artwork for the legacy icons.
magick "$SRC" \( -size "${size}x${size}" xc:none -fill white \
    -draw "roundrectangle 0,0,$((size - 1)),$((size - 1)),$((size * 22 / 100)),$((size * 22 / 100))" \) \
    -compose DstIn -composite "$TMP/square.png"
magick "$SRC" \( -size "${size}x${size}" xc:none -fill white \
    -draw "circle $((size / 2)),$((size / 2)) $((size / 2)),0" \) \
    -compose DstIn -composite "$TMP/round.png"

for d in mdpi:1 hdpi:1.5 xhdpi:2 xxhdpi:3 xxxhdpi:4; do
    name=${d%%:*}
    scale=${d##*:}
    dir="$RES/mipmap-$name"
    mkdir -p "$dir"
    layer=$(awk "BEGIN { print int(108 * $scale) }")
    legacy=$(awk "BEGIN { print int(48 * $scale) }")
    inner=$(awk "BEGIN { print int(44 * $scale) }") # 2dp of padding around a legacy icon
    magick "$TMP/layer.png" -filter Lanczos -resize "${layer}x${layer}" -quality 92 "$dir/ic_launcher_background.webp"
    for kind in square round; do
        out=ic_launcher
        [ "$kind" = round ] && out=ic_launcher_round
        magick "$TMP/$kind.png" -filter Lanczos -resize "${inner}x${inner}" \
            -background none -gravity center -extent "${legacy}x${legacy}" \
            -define webp:lossless=true "$dir/$out.webp"
    done
done

# In-app copy (About, first-run card): the artwork alone, the UI rounds its corners.
mkdir -p "$RES/drawable-nodpi"
magick "$SRC" -filter Lanczos -resize 432x432 -quality 92 "$RES/drawable-nodpi/koan_icon.webp"
