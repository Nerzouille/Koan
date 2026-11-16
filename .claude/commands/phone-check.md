---
description: R8 staging build on the POCO, launch, process death + restore, crash check, short report
argument-hint: "[what to look at after the restore, optional]"
---

Phone check of the current branch on the **POCO X4 GT** (the main test phone), with the R8
**staging** build: release code with R8 on, under its own id `dev.nerzouille.koan.staging`
("Koan Test"), so the real app and its data are never touched. Extra focus from the owner: $ARGUMENTS

Follow these steps in order. Stop and report at the first one that fails.

1. **Branch and tree:** `git branch --show-current` and `git status --short`. Say which commit is
   being checked.
2. **Phone:** `adb devices -l`. Use the POCO (`model:22041216G`) with `-s <serial>` on every adb
   command below. If it isn't connected, stop and ask the owner to plug it in. Never fall back to
   the owner's daily phone.
3. **Build:** `./gradlew assembleStaging` (R8 on, debug-signed). Don't chain it with tests.
4. **Check the id before installing:**
   `$(ls -d ~/Android/Sdk/build-tools/* | sort -V | tail -1)/aapt2 dump packagename app/build/outputs/apk/staging/app-staging.apk`
   must print exactly `dev.nerzouille.koan.staging`. Anything else: stop, don't install.
5. **Install:** `adb -s <serial> install -r app/build/outputs/apk/staging/app-staging.apk`.
   - `INSTALL_FAILED_USER_RESTRICTED`: the phone wants an on-screen approval. Ask the owner to
     tap it, then retry once.
   - Signature mismatch or downgrade (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`,
     `INSTALL_FAILED_VERSION_DOWNGRADE`): stop and tell the owner. Never uninstall to get past it.
6. **Launch:** `adb -s <serial> logcat -c`, then
   `adb -s <serial> shell am start -W -n dev.nerzouille.koan.staging/dev.nerzouille.koan.MainActivity`.
   Report `TotalTime`. Take a screenshot (`adb exec-out screencap -p`) into `app/build/phone-check/`
   (gitignored) and look at it: the app is up, not a blank or crash screen.
7. **Process death:** send the app to the background
   (`adb -s <serial> shell am start -a android.intent.action.MAIN -c android.intent.category.HOME`),
   wait 2 s, then `adb -s <serial> shell am kill dev.nerzouille.koan.staging`. Check it's gone:
   `adb -s <serial> shell pidof dev.nerzouille.koan.staging` prints nothing.
8. **Restore:** start it again the same way as in step 6, screenshot, and compare with step 6:
   same screen, same space and tabs, nothing reset. Add the owner's extra focus here, if they gave one.
   Any tap or swipe follows CLAUDE.md "Driving a phone with adb" (Koan in front + fresh screenshot
   first).
9. **Crash check:**
   `adb -s <serial> logcat -d -b crash` and
   `adb -s <serial> logcat -d | grep -E "FATAL EXCEPTION|AndroidRuntime|ANR in|dev.nerzouille.koan.*(died|crash)"`.
   Quote only the exception lines and the stack frames from Koan's code. Never paste URLs, tab
   titles or account data from the log.
10. **Clean up:** delete the screenshots once they've been checked. Leave the staging app installed.

**Report:** about 6 lines:
- branch and commit;
- build result and APK size;
- id check;
- install result;
- launch time;
- restore OK or not, with what differed;
- crashes found, or none.

Logs, screenshots and dumps are never committed (CLAUDE.md "Public repo").
