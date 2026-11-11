#!/usr/bin/env python3
"""Drive synthetic browser/CCT credential flows on our dedicated, disposable AVD only."""
import argparse
import json
import re
import subprocess
import time
import urllib.request
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser()
parser.add_argument('--adb', default='adb')
parser.add_argument('--serial', default='emulator-5564')
parser.add_argument('--browser', choices=['dev.nerzouille.koan.debug', 'dev.nerzouille.koan.staging'],
                    default='dev.nerzouille.koan.debug')
parser.add_argument('--mode', choices=['all', 'browser', 'custom-tab', 'autofill'], default='all')
parser.add_argument('--default-browser', action='store_true', help='Launch normal links without a package')
args = parser.parse_args()

def adb(*cmd):
    return subprocess.check_output([args.adb, '-s', args.serial, *cmd], text=True, timeout=20).strip()

assert args.serial.startswith('emulator-')
assert adb('emu', 'avd', 'name').splitlines()[0] == 'Koan_Passkey_Test'
assert adb('shell', 'getprop', 'ro.kernel.qemu') == '1'

def state(path='/state'):
    with urllib.request.urlopen('http://127.0.0.1:8786' + path, timeout=3) as response:
        return json.load(response)

def nodes():
    adb('shell', 'uiautomator', 'dump', '/sdcard/koan-fixture-ui.xml')
    return list(ET.fromstring(adb('shell', 'cat', '/sdcard/koan-fixture-ui.xml')).iter('node'))

def click(tree, text):
    for node in tree:
        if node.get('text') == text and node.get('enabled') == 'true':
            x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
            if x2 > x1 and y2 > y1:
                adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
                return True
    return False

def wait_flow(fill=False):
    deadline = time.monotonic() + 180
    cancel_sent_at = 0.0
    selected = False
    last_phase = None
    while time.monotonic() < deadline:
        status = state()
        assert not status['error'], status['error']
        if status['passed']:
            return
        if status['phase'] != last_phase:
            print('Fixture phase:', status['phase'], flush=True)
            last_phase = status['phase']
        tree = nodes()
        # Dumping the tree takes time; do not confirm a newly opened cancellation prompt
        # using the preceding authentication phase's stale state.
        status = state()
        assert not status['error'], status['error']
        if status['passed']:
            return
        packages = {n.get('package', '') for n in tree}
        picker = any('credential' in p or 'permissioncontroller' in p for p in packages)
        if fill:
            if click(tree, 'Koan test login'):
                selected = True
            elif selected:
                click(tree, 'Check password fill')
            else:
                click(tree, 'Focus username')
        elif status['phase'] == 'cancel' and picker:
            # The preceding login sheet can still be animating away after the website has
            # entered the cancellation phase. Back there would navigate/close the page.
            cancel_sheet = any(n.get('text', '').startswith('cancel-fixture') for n in tree)
            if cancel_sheet and time.monotonic() - cancel_sent_at > 3:
                adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')
                cancel_sent_at = time.monotonic()
                print('Canceled the identified fixture sheet', flush=True)
        elif picker:
            for label in ('Continue', 'Create', 'Save', 'Koan test account'):
                if click(tree, label):
                    break
        elif args.browser in packages:
            click(tree, 'Run passkey check')
        time.sleep(.2)
    raise AssertionError('Fixture timed out at ' + str(state()))

adb('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP')
for mode in (('browser', 'custom-tab', 'autofill') if args.mode == 'all' else (args.mode,)):
    state('/reset')
    if mode == 'custom-tab':
        # Start a fresh external client even when this script was run before. All browser
        # requests from the preceding mode have completed before stopping the fixture.
        adb('shell', 'am', 'force-stop', 'dev.nerzouille.koan.credentials.fixture')
        adb('shell', 'am', 'start', '-n', 'dev.nerzouille.koan.credentials.fixture/dev.nerzouille.koan.auth.testing.CustomTabFixture',
            '--es', 'browser', args.browser)
    else:
        url = 'http://localhost:8786/' + ('autofill' if mode == 'autofill' else '')
        command = ['shell', 'am', 'start', '-a', 'android.intent.action.VIEW', '-d', url]
        if not args.default_browser:
            command += ['-p', args.browser]
        adb(*command)
    wait_flow(fill=mode == 'autofill')
    print('PASS:', mode, flush=True)
