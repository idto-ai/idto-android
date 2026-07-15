package ai.idto.sdk.example

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import ai.idto.sdk.IDtoAbandonData
import ai.idto.sdk.IDtoDecisionMode
import ai.idto.sdk.IDtoErrorData
import ai.idto.sdk.IDtoFaceMatchConfig
import ai.idto.sdk.IDtoNameMatchConfig
import ai.idto.sdk.IDtoStepCompleteData
import ai.idto.sdk.IDtoTokenProvider
import ai.idto.sdk.IDtoWorkflowCompleteData
import ai.idto.sdk.landing.IDtoLandingConfig
import ai.idto.sdk.landing.IDtoLandingListener
import ai.idto.sdk.landing.IDtoLandingView

class MainActivity : Activity() {

    private val tokenProvider = IDtoTokenProvider { callback ->
        Thread {
            try {
                callback.onToken(
                    SdkTokenClient(DevCredentials.BASE_URL)
                        .fetchClientToken(DevCredentials.CLIENT_ID, DevCredentials.CLIENT_SECRET),
                )
            } catch (t: Throwable) {
                callback.onError(t)
            }
        }.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!DevCredentials.isConfigured) {
            toast("Set demo credentials — see example/README")
            return
        }
        val landing = IDtoLandingView(this)
        landing.configure(buildConfig())
        landing.setListener(object : IDtoLandingListener {
            override fun onStepComplete(data: IDtoStepCompleteData) = toast("step ${data.step}")
            override fun onComplete(data: IDtoWorkflowCompleteData) = toast("complete")
            override fun onAbandon(data: IDtoAbandonData) = toast("abandon ${data.atStep}")
            override fun onError(data: IDtoErrorData) = toast("error ${data.step}: ${data.error}")
            override fun onDismiss() = toast("dismissed")
        })
        setContentView(landing)
    }

    private fun buildConfig(): IDtoLandingConfig =
        IDtoLandingConfig.Builder(DevCredentials.WORKFLOW_TEMPLATE_ID, tokenProvider)
            .businessName(DevCredentials.BUSINESS_NAME)
            .brandColor(DevCredentials.BRAND_COLOR)
            .colors(DevCredentials.colors())
            .logo(resources.getDrawable(R.drawable.drivex, theme))
            .logoSizeDp(110, 36)
            .logoUrl(DrivexLogo.DATA_URI)
            .env(DevCredentials.ENV)
            .baseUrl(DevCredentials.BASE_URL)
            .phone(DevCredentials.SAMPLE_PHONE)
            .faceMatchConfig(IDtoFaceMatchConfig.Builder().skipLiveness(false).build())
            .nameMatchConfig(
                IDtoNameMatchConfig.Builder().threshold(80).decisionMode(IDtoDecisionMode.ALL).build(),
            )
            .debug(true)
            .build()

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
