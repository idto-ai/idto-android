package ai.idto.sdk.example

import ai.idto.sdk.IDtoColors
import ai.idto.sdk.IDtoEnv

/*
 * TEST-ONLY demo configuration for the example app.
 *
 * NEVER ship a client_secret inside a real app. In production your backend holds
 * the secret, calls POST /auth/sdk/token, and returns only the short-lived
 * client_token to the app.
 *
 * The secret trio (client_id / client_secret / workflow_template_id) is injected
 * at build time from env vars, Gradle properties, or a git-ignored
 * example/demo.properties file — never committed. See example/README.md.
 */
object DevCredentials {

    const val BASE_URL = "https://prod.idto.ai"
    val ENV = IDtoEnv.PRODUCTION

    val CLIENT_ID: String = BuildConfig.IDTO_DEMO_CLIENT_ID
    val CLIENT_SECRET: String = BuildConfig.IDTO_DEMO_CLIENT_SECRET
    val WORKFLOW_TEMPLATE_ID: String = BuildConfig.IDTO_DEMO_WORKFLOW_ID

    val isConfigured: Boolean
        get() = CLIENT_ID.isNotBlank() &&
            CLIENT_SECRET.isNotBlank() &&
            WORKFLOW_TEMPLATE_ID.isNotBlank()

    const val BUSINESS_NAME = "DriveX"
    const val BRAND_COLOR = "#E8452F"
    const val SAMPLE_PHONE = "9999999999"

    fun colors(): IDtoColors = IDtoColors.Builder()
        .primary(BRAND_COLOR)
        .background("#ffffff")
        .text("#18181b")
        .text2("#71717a")
        .border("#e5e5e5")
        .buttonTextColorPrimary("#ffffff")
        .buttonTextColorSecondary("#ffffff")
        .build()
}
