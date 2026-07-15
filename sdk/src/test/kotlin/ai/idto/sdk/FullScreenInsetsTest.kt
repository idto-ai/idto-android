package ai.idto.sdk

import ai.idto.sdk.internal.fullScreenPadding
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class FullScreenInsetsTest {

    @Test
    fun `bottom padding is system bars when ime hidden`() {
        assertArrayEquals(intArrayOf(10, 60, 20, 40), fullScreenPadding(10, 60, 20, 40, 0))
    }

    @Test
    fun `bottom padding is ime when it exceeds system bars`() {
        assertArrayEquals(intArrayOf(0, 60, 0, 900), fullScreenPadding(0, 60, 0, 40, 900))
    }

    @Test
    fun `bottom padding takes the max not the sum`() {
        assertArrayEquals(intArrayOf(0, 0, 0, 500), fullScreenPadding(0, 0, 0, 500, 300))
    }
}
