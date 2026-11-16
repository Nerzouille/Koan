package dev.nerzouille.koan.baselineprofile

import android.content.Intent
import android.net.Uri
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Koan's hot paths for the baseline profile: start, the tab stack, a page, the search sheet, the
 * ⋯ menu and Settings. Runs on a fresh copy (no account, one local space); pages come from a demo
 * server on the computer through `adb reverse tcp:8765 tcp:8765`, never from the internet.
 * Every step is best effort: a missed tap only leaves that path out of the profile.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    private val target: String = InstrumentationRegistry.getArguments().getString("targetAppId")
        ?.takeIf { it.isNotEmpty() } ?: error("targetAppId missing")

    @Test
    fun generate() {
        // Never the real app: the plugin's build types carry their own id (app/build.gradle.kts).
        check(target != "dev.nerzouille.koan") { "refusing to drive the real app" }
        rule.collect(packageName = target, includeInStartupProfile = true) {
            pressHome()
            startActivityAndWait()
            device.inFront(target) { skipTour() }
            openPage(device, target, "http://127.0.0.1:8765/page1.html")
            openPage(device, target, "http://127.0.0.1:8765/page2.html")
            device.inFront(target) { stack() }
            device.inFront(target) { search() }
            device.inFront(target) { menu() }
            device.inFront(target) { settings() }
        }
    }

    private fun openPage(device: UiDevice, pkg: String, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        InstrumentationRegistry.getInstrumentation().context.startActivity(intent)
        device.waitForIdle()
        Thread.sleep(PAGE_MS)
    }
}

private const val PAGE_MS = 2_500L

/** Runs [step] only while the copy under test is in front: never a tap into another app. */
private fun UiDevice.inFront(pkg: String, step: UiDevice.() -> Unit) {
    waitForIdle()
    if (currentPackageName == pkg) step() else android.util.Log.w("KoanBaselineProfile", "skipped a step: $currentPackageName in front")
}
private const val STEP_MS = 900L

private fun UiDevice.pause() {
    waitForIdle()
    Thread.sleep(STEP_MS)
}

private fun UiDevice.skipTour() {
    wait(Until.findObject(By.text("Skip")), 5_000)?.click()
    pause()
}

private fun UiDevice.dockY() = (displayHeight * 0.955).toInt()

/** Swipe up on the dock: the tab stack; scroll it, open the front card. */
private fun UiDevice.stack() {
    val cx = displayWidth / 2
    swipe(cx, dockY(), cx, displayHeight / 3, 12)
    pause()
    repeat(3) {
        swipe(cx, displayHeight / 3, cx, displayHeight * 2 / 3, 20)
        pause()
        swipe(cx, displayHeight * 2 / 3, cx, displayHeight / 3, 20)
        pause()
    }
    click(cx, displayHeight * 2 / 3)
    pause()
}

/** Tap the address pill: the search sheet; type, then leave. */
private fun UiDevice.search() {
    click(displayWidth / 2, dockY())
    pause()
    executeShellCommand("input text wiki")
    pause()
    pressBack()
    pause()
    pressBack()
    pause()
}

/** ⋯ in the dock: the menu sheet. */
private fun UiDevice.menu() {
    click((displayWidth * 0.9).toInt(), dockY())
    pause()
    pressBack()
    pause()
}

/** Long press ⋯: Settings; scroll it and back out. */
private fun UiDevice.settings() {
    val x = (displayWidth * 0.9).toInt()
    swipe(x, dockY(), x, dockY(), 120) // a long press
    pause()
    swipe(displayWidth / 2, displayHeight * 3 / 4, displayWidth / 2, displayHeight / 4, 20)
    pause()
    pressBack()
    pause()
}
