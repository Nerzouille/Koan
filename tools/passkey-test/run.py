#!/usr/bin/env python3
"""Temporarily configure ONLY Koan_Passkey_Test; restore services/default browser on exit."""
import argparse
from pathlib import Path
import subprocess
import sys
import time

parser = argparse.ArgumentParser()
parser.add_argument('--adb', default='adb')
parser.add_argument('--serial', default='emulator-5564')
parser.add_argument('--browser', choices=['dev.nerzouille.koan.debug', 'dev.nerzouille.koan.staging'],
                    default='dev.nerzouille.koan.debug')
args = parser.parse_args()
here = Path(__file__).resolve().parent

def adb(*cmd):
    return subprocess.check_output([args.adb, '-s', args.serial, *cmd], text=True, timeout=30).strip()

assert args.serial.startswith('emulator-')
assert adb('emu', 'avd', 'name').splitlines()[0] == 'Koan_Passkey_Test'
assert adb('shell', 'getprop', 'ro.kernel.qemu') == '1'
assert not adb('reverse', '--list'), 'Use a disposable AVD with no existing reverse tunnels'
keys = ['credential_service', 'credential_service_primary', 'autofill_service']
saved = {key: adb('shell', 'settings', 'get', 'secure', key) for key in keys}
stay_awake = adb('shell', 'settings', 'get', 'global', 'stay_on_while_plugged_in')
holders = adb('shell', 'cmd', 'role', 'get-role-holders', 'android.app.role.BROWSER').splitlines()
gms_disabled = 'package:com.google.android.gms' in adb('shell', 'pm', 'list', 'packages', '-d').splitlines()
gms_present = 'package:com.google.android.gms' in adb('shell', 'pm', 'list', 'packages', 'com.google.android.gms').splitlines()
provider = 'dev.nerzouille.koan.credentials.fixture/dev.nerzouille.koan.auth.testing.TestCredentialProvider'
autofill = 'dev.nerzouille.koan.credentials.fixture/dev.nerzouille.koan.auth.testing.TestAutofillService'
server = subprocess.Popen(['node', str(here / 'server.mjs')], cwd=here)
try:
    time.sleep(.3)
    assert server.poll() is None, 'Local test server failed (port 8786 must be free)'
    adb('reverse', 'tcp:8786', 'tcp:8786')
    for key in keys:
        adb('shell', 'settings', 'put', 'secure', key, autofill if key == 'autofill_service' else provider)
    adb('shell', 'settings', 'put', 'global', 'stay_on_while_plugged_in', '7')
    if gms_present:
        adb('shell', 'pm', 'disable-user', '--user', '0', 'com.google.android.gms')
    adb('shell', 'cmd', 'role', 'add-role-holder', 'android.app.role.BROWSER', args.browser)
    resolved = adb('shell', 'cmd', 'package', 'resolve-activity', '--brief', '-a', 'android.intent.action.VIEW',
                   '-d', 'http://localhost:8786/')
    assert args.browser + '/' in resolved, 'System default link resolution did not select Koan'
    subprocess.run([sys.executable, str(here / 'drive.py'), '--adb', args.adb, '--serial', args.serial,
                    '--browser', args.browser, '--default-browser'], check=True)
finally:
    for key, value in saved.items():
        adb('shell', 'settings', 'delete' if value == 'null' else 'put', 'secure', key,
            *([] if value == 'null' else [value]))
    adb('shell', 'settings', 'delete' if stay_awake == 'null' else 'put', 'global', 'stay_on_while_plugged_in',
        *([] if stay_awake == 'null' else [stay_awake]))
    if gms_present and not gms_disabled:
        adb('shell', 'pm', 'enable', 'com.google.android.gms')
    adb('shell', 'cmd', 'role', 'clear-role-holders', 'android.app.role.BROWSER')
    for holder in holders:
        adb('shell', 'cmd', 'role', 'add-role-holder', 'android.app.role.BROWSER', holder)
    adb('reverse', '--remove', 'tcp:8786')
    server.terminate()
    server.wait(timeout=5)
