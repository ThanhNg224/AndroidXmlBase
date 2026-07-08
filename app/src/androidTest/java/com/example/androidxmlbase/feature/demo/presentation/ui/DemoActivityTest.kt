package com.example.androidxmlbase.feature.demo.presentation.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidxmlbase.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DemoActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(DemoActivity::class.java)

    @Test
    fun incrementButton_updatesCountText() {
        onView(withId(R.id.tvCount)).check(matches(withText("0")))

        repeat(10) {
            onView(withId(R.id.btnIncrement)).perform(click())
        }

        onView(withId(R.id.tvCount)).check(matches(withText("10")))
    }
}
