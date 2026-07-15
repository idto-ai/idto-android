package ai.idto.sdk

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class JsonCanaryTest {
    @Test
    fun jsonObjectSerializesRealImplementation() {
        assertEquals("""{"a":1}""", JSONObject().put("a", 1).toString())
    }
}
