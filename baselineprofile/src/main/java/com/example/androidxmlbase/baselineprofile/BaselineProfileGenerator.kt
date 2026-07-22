package com.example.androidxmlbase.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the app's critical startup path (cold launch → open the demo screen → back) so the
 * generated `baseline-prof.txt` covers real code paths, not just process init.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateStartupProfile() =
        baselineProfileRule.collect(packageName = "com.example.androidxmlbase") {
            pressHome()
            startActivityAndWait()

            device.wait(Until.hasObject(By.res(packageName, "btnOpenDemo")), 5_000)
            device.findObject(By.res(packageName, "btnOpenDemo"))?.click()
            device.wait(Until.hasObject(By.pkg(packageName).depth(0)), 5_000)
            device.pressBack()
        }
}
