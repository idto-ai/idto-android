package ai.idto.sdk

import android.app.Activity
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoboCanaryTest {
    @Test
    fun bootsTrivialActivity() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        assertNotNull(activity)
    }
}
