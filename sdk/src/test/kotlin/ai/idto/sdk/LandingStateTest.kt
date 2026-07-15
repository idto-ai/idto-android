package ai.idto.sdk

import ai.idto.sdk.landing.LandingController
import ai.idto.sdk.landing.LandingDefaults
import ai.idto.sdk.landing.LandingEvent
import ai.idto.sdk.landing.LandingState
import ai.idto.sdk.landing.LandingStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class LandingStateTest {

    private fun next(s: LandingStatus, e: LandingEvent) = LandingState.next(s, e)

    @Test fun idleStartToLoading() =
        assertEquals(LandingStatus.LOADING, next(LandingStatus.IDLE, LandingEvent.START))

    @Test fun loadingTokenOkToOpen() =
        assertEquals(LandingStatus.OPEN, next(LandingStatus.LOADING, LandingEvent.TOKEN_OK))

    @Test fun loadingTokenErrorToIdle() =
        assertEquals(LandingStatus.IDLE, next(LandingStatus.LOADING, LandingEvent.TOKEN_ERROR))

    @Test fun startWhileLoadingNoOp() =
        assertEquals(LandingStatus.LOADING, next(LandingStatus.LOADING, LandingEvent.START))

    @Test fun startWhileOpenNoOp() =
        assertEquals(LandingStatus.OPEN, next(LandingStatus.OPEN, LandingEvent.START))

    @Test fun openWorkflowCompleteToDone() =
        assertEquals(LandingStatus.DONE, next(LandingStatus.OPEN, LandingEvent.WORKFLOW_COMPLETE))

    @Test fun openAbandonToIdle() =
        assertEquals(LandingStatus.IDLE, next(LandingStatus.OPEN, LandingEvent.ABANDON))

    @Test fun openCloseToIdle() =
        assertEquals(LandingStatus.IDLE, next(LandingStatus.OPEN, LandingEvent.CLOSE))

    @Test fun doneCloseStaysDone() =
        assertEquals(LandingStatus.DONE, next(LandingStatus.DONE, LandingEvent.CLOSE))

    @Test fun doneStartToLoading() =
        assertEquals(LandingStatus.LOADING, next(LandingStatus.DONE, LandingEvent.START))

    @Test fun controllerFullHappyPath() {
        val c = LandingController()
        assertEquals(LandingStatus.IDLE, c.status)
        c.dispatch(LandingEvent.START)
        assertEquals(LandingStatus.LOADING, c.status)
        c.dispatch(LandingEvent.TOKEN_OK)
        assertEquals(LandingStatus.OPEN, c.status)
        c.dispatch(LandingEvent.WORKFLOW_COMPLETE)
        assertEquals(LandingStatus.DONE, c.status)
        c.dispatch(LandingEvent.CLOSE)
        assertEquals(LandingStatus.DONE, c.status)
    }

    @Test fun controllerGetTokenThrowReturnsIdle() {
        val c = LandingController()
        c.dispatch(LandingEvent.START)
        c.dispatch(LandingEvent.TOKEN_ERROR)
        assertEquals(LandingStatus.IDLE, c.status)
    }

    @Test fun ctaLabelPerState() {
        val c = LandingController()
        val cta = LandingDefaults.DEFAULT_COPY.cta
        assertEquals(cta.idle, c.ctaLabel())
        c.dispatch(LandingEvent.START)
        assertEquals(cta.loading, c.ctaLabel())
        c.dispatch(LandingEvent.TOKEN_OK)
        assertEquals(cta.open, c.ctaLabel())
        c.dispatch(LandingEvent.WORKFLOW_COMPLETE)
        assertEquals(cta.done, c.ctaLabel())
    }
}
