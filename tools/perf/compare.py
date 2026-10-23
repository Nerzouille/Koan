#!/usr/bin/env python3
"""Side by side summary of frames.sh / start.sh runs. Usage: compare.py <label> [<label> ...]

Per scenario: median over rounds of frames, janky % (with min-max) and the p50/90/95/99 frame
times reported by gfxinfo; then cold and warm start times.
"""
import glob
import os
import re
import statistics
import sys

OUT = os.environ.get('KOAN_PERF_OUT', os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../app/build/perf'))


def frames(path):
    t = open(path).read()
    total = int(re.search(r'Total frames rendered: (\d+)', t).group(1))
    janky = float(re.search(r'Janky frames: \d+ \(([\d.]+)%\)', t).group(1))
    pct = [int(re.search(rf'{q}th percentile: (\d+)ms', t).group(1)) for q in (50, 90, 95, 99)]
    return total, janky, pct


def main(labels):
    med = statistics.median
    for label in labels:
        d = os.path.join(OUT, label)
        print(f'== {label}')
        rows = {}
        for f in sorted(glob.glob(os.path.join(d, '*-*.txt'))):
            rows.setdefault(os.path.basename(f).rsplit('-', 1)[0], []).append(frames(f))
        for scen, rs in rows.items():
            p = '/'.join(str(int(med([r[2][i] for r in rs]))) for i in range(4))
            print(f'  {scen:7} rounds={len(rs)} frames~{med([r[0] for r in rs]):.0f} '
                  f'janky%={med([r[1] for r in rs]):5.1f} (min {min(r[1] for r in rs):.1f} '
                  f'max {max(r[1] for r in rs):.1f})  p50/90/95/99={p} ms')
        start = os.path.join(d, 'start')
        if os.path.exists(start):
            times = {}
            for line in open(start):
                if 'TotalTime' not in line:
                    continue
                kind, _, _, ms = line.split()
                times.setdefault(kind, []).append(int(ms))
            for kind, ms in times.items():
                print(f'  {kind} start: median {med(ms):.0f} ms (min {min(ms)} max {max(ms)})')


if __name__ == '__main__':
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    main(sys.argv[1:])
