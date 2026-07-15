package ai.idto.sdk

import ai.idto.sdk.internal.SessionRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionRegistryTest {

    private val listener = object : IDtoEventListener {}
    private fun config() = IDtoConfig.Builder("token", "wft").build()

    @Before
    fun setUp() = SessionRegistry.clear()

    @After
    fun tearDown() = SessionRegistry.clear()

    @Test
    fun `register then lookup returns same entry`() {
        val entry = SessionRegistry.register(config(), listener, null)!!
        assertSame(entry, SessionRegistry.get(entry.id))
    }

    @Test
    fun `remove clears the entry`() {
        val entry = SessionRegistry.register(config(), listener, null)!!
        SessionRegistry.remove(entry.id)
        assertNull(SessionRegistry.get(entry.id))
    }

    @Test
    fun `second register while active is rejected`() {
        SessionRegistry.register(config(), listener, null)!!
        assertNull(SessionRegistry.register(config(), listener, null))
    }

    @Test
    fun `register after remove succeeds`() {
        val first = SessionRegistry.register(config(), listener, null)!!
        SessionRegistry.remove(first.id)
        val second = SessionRegistry.register(config(), listener, null)
        assertEquals(second!!.id, SessionRegistry.get(second.id)!!.id)
    }

    @Test
    fun `isActive reflects registry state`() {
        assertFalse(SessionRegistry.isActive())
        val entry = SessionRegistry.register(config(), listener, null)!!
        assertTrue(SessionRegistry.isActive())
        SessionRegistry.remove(entry.id)
        assertFalse(SessionRegistry.isActive())
    }

    @Test
    fun `get with null id returns null`() {
        assertNull(SessionRegistry.get(null))
    }
}
