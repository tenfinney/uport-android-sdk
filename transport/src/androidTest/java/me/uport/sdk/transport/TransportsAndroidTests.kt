package me.uport.sdk.transport

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.intent.Intents.intended
import android.support.test.espresso.intent.matcher.IntentMatchers.hasAction
import android.support.test.espresso.intent.matcher.IntentMatchers.hasCategories
import android.support.test.espresso.intent.matcher.IntentMatchers.hasData
import android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra
import android.support.test.espresso.intent.matcher.UriMatchers.hasHost
import android.support.test.espresso.intent.matcher.UriMatchers.hasPath
import android.support.test.espresso.intent.rule.IntentsTestRule
import me.uport.sdk.transport.RequestDispatchActivity.Companion.ACTION_DISPATCH_REQUEST
import me.uport.sdk.transport.RequestDispatchActivity.Companion.EXTRA_REQUEST_URI
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

//TODO: move this to JVM once robolectric works properly with espresso intents
class TransportsAndroidTests {

    //only used for testing
    class DummyActivity : Activity()

    @get:Rule
    var intentsTestRule = IntentsTestRule<DummyActivity>(DummyActivity::class.java, true, true)

    private val filter: IntentFilter? = null
    // used to block intent from actual broadcast
    private var instrumentation: Instrumentation? = null
    private var monitor: Instrumentation.ActivityMonitor? = null

    @Before
    fun run_before_every_test() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        monitor = instrumentation?.addMonitor(filter, null, true)
    }

    @After
    fun run_after_every_test() {
        instrumentation?.removeMonitor(monitor)
    }

    @Test
    fun share_req_sends_intent_with_proper_data() {

        val jwt = "dummy.jwt.bundle"
        Transports().send(intentsTestRule.activity, jwt)

        intended(allOf(
                hasAction(equalTo(Intent.ACTION_VIEW)),
                hasCategories(hasItem(equalTo(Intent.CATEGORY_BROWSABLE))),
                hasData(allOf(
                        hasHost(equalTo("id.uport.me")),
                        hasPath(containsString("/req/$jwt"))
                )
                )))
    }

    @Test
    fun share_req_with_response_sends_intent_with_proper_data() {
        val jwt = "dummy.jwt.bundle"
        Transports().sendExpectingResult(intentsTestRule.activity, jwt)

        intended(allOf(
                hasAction(equalTo(ACTION_DISPATCH_REQUEST)),
                hasExtra(equalTo(EXTRA_REQUEST_URI), containsString(jwt))
        ))
    }

}