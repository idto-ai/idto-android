package ai.idto.sdk.landing

enum class LandingStatus { IDLE, LOADING, OPEN, DONE }

enum class LandingEvent { START, TOKEN_OK, TOKEN_ERROR, WORKFLOW_COMPLETE, ABANDON, CLOSE }

object LandingState {
    fun next(status: LandingStatus, event: LandingEvent): LandingStatus = when (event) {
        LandingEvent.START -> if (status == LandingStatus.LOADING || status == LandingStatus.OPEN) status else LandingStatus.LOADING
        LandingEvent.TOKEN_OK -> if (status == LandingStatus.LOADING) LandingStatus.OPEN else status
        LandingEvent.TOKEN_ERROR -> if (status == LandingStatus.LOADING) LandingStatus.IDLE else status
        LandingEvent.WORKFLOW_COMPLETE -> if (status == LandingStatus.OPEN) LandingStatus.DONE else status
        LandingEvent.ABANDON -> LandingStatus.IDLE
        LandingEvent.CLOSE -> if (status == LandingStatus.DONE) LandingStatus.DONE else LandingStatus.IDLE
    }
}

class LandingController(private val copy: IDtoLandingCopy = LandingDefaults.DEFAULT_COPY) {
    var status: LandingStatus = LandingStatus.IDLE
        private set

    fun dispatch(event: LandingEvent) {
        status = LandingState.next(status, event)
    }

    fun ctaLabel(): String = when (status) {
        LandingStatus.IDLE -> copy.cta.idle
        LandingStatus.LOADING -> copy.cta.loading
        LandingStatus.OPEN -> copy.cta.open
        LandingStatus.DONE -> copy.cta.done
    }
}
