package ai.idto.sdk

import ai.idto.sdk.internal.NavigationPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationPolicyTest {

    private fun allowed(url: String, baseUrl: String? = null, extra: List<String>? = null) =
        NavigationPolicy.isAllowedNavigation(url, baseUrl, extra)

    @Test fun aboutScheme() = assertTrue(allowed("about:blank"))
    @Test fun dataScheme() = assertTrue(allowed("data:text/html,<h1>hi</h1>"))
    @Test fun blobScheme() = assertTrue(allowed("blob:https://prod.idto.ai/abc"))

    @Test fun cdnHost() =
        assertTrue(allowed("https://idto-sdk-bucket.s3.ap-south-1.amazonaws.com/sdk/prod/idto.js"))

    @Test fun idtoApex() = assertTrue(allowed("https://idto.ai/"))
    @Test fun idtoSubdomainProd() = assertTrue(allowed("https://prod.idto.ai/api"))
    @Test fun idtoSubdomainDev() = assertTrue(allowed("https://dev.idto.ai/api"))
    @Test fun idtoSubdomainDigilocker() = assertTrue(allowed("https://digilocker.idto.ai/oauth"))
    @Test fun digilockerGov() = assertTrue(allowed("https://digilocker.gov.in/"))
    @Test fun digilockerGovSubdomain() = assertTrue(allowed("https://api.digilocker.gov.in/x"))
    @Test fun meripehchaanGov() = assertTrue(allowed("https://meripehchaan.gov.in/"))
    @Test fun meripehchaanSubdomain() = assertTrue(allowed("https://digilocker.meripehchaan.gov.in/x"))

    @Test fun suffixAttackIdto() = assertFalse(allowed("https://idto.ai.evil.com/"))
    @Test fun suffixAttackDigilocker() = assertFalse(allowed("https://xdigilocker.gov.in/"))
    @Test fun suffixAttackEvilIdto() = assertFalse(allowed("https://evilidto.ai/"))
    @Test fun userinfoTrick() = assertFalse(allowed("https://idto.ai@evil.com/"))

    @Test fun arbitraryHostBlocked() = assertFalse(allowed("https://google.com/"))
    @Test fun malformedBlocked() = assertFalse(allowed("https://exa mple.com/"))
    @Test fun javascriptSchemeBlocked() = assertFalse(allowed("javascript:alert(1)"))
    @Test fun noSchemeBlocked() = assertFalse(allowed("//idto.ai/x"))
    @Test fun emptyHostHttpsBlocked() = assertFalse(allowed("https:///path"))

    @Test fun baseUrlOverrideHostAllowed() =
        assertTrue(allowed("https://staging.example.com/x", baseUrl = "https://staging.example.com"))

    @Test fun baseUrlOverrideDifferentHostBlocked() =
        assertFalse(allowed("https://other.example.com/x", baseUrl = "https://staging.example.com"))

    @Test fun extraHostExactMatchAllowed() =
        assertTrue(allowed("https://partner.co/x", extra = listOf("partner.co")))

    @Test fun extraHostCaseInsensitive() =
        assertTrue(allowed("https://Partner.CO/x", extra = listOf("partner.co")))

    @Test fun extraHostSubdomainNotMatched() =
        assertFalse(allowed("https://sub.partner.co/x", extra = listOf("partner.co")))

    @Test fun digiLockerUrlProxy() = assertTrue(NavigationPolicy.isDigiLockerUrl("https://digilocker.idto.ai/oauth"))
    @Test fun digiLockerUrlGov() = assertTrue(NavigationPolicy.isDigiLockerUrl("https://x.digilocker.gov.in/a"))
    @Test fun digiLockerUrlMeri() = assertTrue(NavigationPolicy.isDigiLockerUrl("https://digilocker.meripehchaan.gov.in/a"))
    @Test fun digiLockerUrlGovApex() = assertTrue(NavigationPolicy.isDigiLockerUrl("https://digilocker.gov.in/a"))
    @Test fun digiLockerUrlProdFalse() = assertFalse(NavigationPolicy.isDigiLockerUrl("https://prod.idto.ai/a"))
    @Test fun digiLockerUrlSuffixFalse() = assertFalse(NavigationPolicy.isDigiLockerUrl("https://digilocker.gov.in.evil.com/a"))

    @Test fun webViewOriginProd() =
        assertEquals("https://prod.idto.ai", NavigationPolicy.webViewOrigin(IDtoEnv.PRODUCTION, null))
    @Test fun webViewOriginDev() =
        assertEquals("https://dev.idto.ai", NavigationPolicy.webViewOrigin(IDtoEnv.DEVELOPMENT, null))
    @Test fun webViewOriginNullDefaultsProd() =
        assertEquals("https://prod.idto.ai", NavigationPolicy.webViewOrigin(null, null))
    @Test fun webViewOriginBaseUrlStripsPath() =
        assertEquals("https://custom.host.com", NavigationPolicy.webViewOrigin(IDtoEnv.PRODUCTION, "https://custom.host.com/deep/path"))
    @Test fun webViewOriginBaseUrlWithPort() =
        assertEquals("https://custom.host.com:8443", NavigationPolicy.webViewOrigin(IDtoEnv.PRODUCTION, "https://custom.host.com:8443/x"))
    @Test fun webViewOriginBadBaseUrlFallsBack() =
        assertEquals("https://dev.idto.ai", NavigationPolicy.webViewOrigin(IDtoEnv.DEVELOPMENT, "not a url"))
}
