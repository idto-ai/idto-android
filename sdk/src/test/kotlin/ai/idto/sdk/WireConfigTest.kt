package ai.idto.sdk

import ai.idto.sdk.internal.WireConfig
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WireConfigTest {

    private fun minimal() = IDtoConfig.Builder("ct-123", "wf-456").build()

    private fun full() = IDtoConfig.Builder("ct-123", "wf-456")
        .sessionToken("sess")
        .merchantUserId("mu")
        .phone("+919876543210")
        .startFresh(true)
        .preVerified(mapOf("pan" to true, "aadhaar" to false))
        .accumulatedData(JSONObject().put("k", "v"))
        .aadhaarConfig(IDtoAadhaarConfig.Builder().digilockerMaxFailures(1).okycEnabled(true).build())
        .referenceName("Ada Lovelace")
        .nameMatchConfig(
            IDtoNameMatchConfig.Builder()
                .threshold(80)
                .decisionMode(IDtoDecisionMode.ANY)
                .extraHonorifics(listOf("late"))
                .aliases(mapOf("mohammad" to "mohammed"))
                .comparePairs(
                    listOf(
                        IDtoComparePair.AADHAAR_VS_PAN,
                        IDtoComparePair.AADHAAR_VS_DL,
                        IDtoComparePair.AADHAAR_VS_BANK,
                        IDtoComparePair.PAN_VS_BANK,
                    )
                )
                .build()
        )
        .faceMatchReferenceImage("https://img")
        .faceMatchConfig(
            IDtoFaceMatchConfig.Builder()
                .skipLiveness(false)
                .threshold(75)
                .livenessFailurePolicy(IDtoLivenessPolicy.FAIL_CLOSED)
                .build()
        )
        .panConfig(IDtoPanConfig.Builder().skipContextScreen(true).build())
        .businessName("Acme")
        .logo("https://logo")
        .language(IDtoLanguage.HI)
        .theme(IDtoTheme.DARK)
        .displayMode(IDtoDisplayMode.BOTTOM_SHEET)
        .bottomSheet(IDtoBottomSheet.Builder().minHeight("60%").maxHeight("90%").build())
        .env(IDtoEnv.DEVELOPMENT)
        .baseUrl("https://custom.idto.ai")
        .colors(
            IDtoColors.Builder()
                .background("#000").text("#111").text2("#222").border("#333")
                .primary("#444").secondary("#555")
                .buttonTextColorPrimary("#666").buttonTextColorSecondary("#777")
                .build()
        )
        .debug(true)
        .readyTimeoutMs(9999)
        .allowedHosts(listOf("extra.example.com"))
        .build()

    @Test fun clientTokenRenamed() =
        assertEquals("ct-123", WireConfig.toWebConfig(minimal()).getString("client_token"))

    @Test fun workflowTemplateIdRenamed() =
        assertEquals("wf-456", WireConfig.toWebConfig(minimal()).getString("workflow_template_id"))

    @Test fun sessionTokenRenamed() =
        assertEquals("sess", WireConfig.toWebConfig(full()).getString("session_token"))

    @Test fun merchantUserIdRenamed() =
        assertEquals("mu", WireConfig.toWebConfig(full()).getString("merchant_user_id"))

    @Test fun startFreshRenamed() =
        assertTrue(WireConfig.toWebConfig(full()).getBoolean("start_fresh"))

    @Test fun preVerifiedRenamedToObjectOfBooleans() {
        val pv = WireConfig.toWebConfig(full()).getJSONObject("pre_verified")
        assertTrue(pv.getBoolean("pan"))
        assertFalse(pv.getBoolean("aadhaar"))
    }

    @Test fun accumulatedDataRenamedAndPassedThrough() =
        assertEquals("v", WireConfig.toWebConfig(full()).getJSONObject("accumulated_data").getString("k"))

    @Test fun referenceNameRenamed() =
        assertEquals("Ada Lovelace", WireConfig.toWebConfig(full()).getString("reference_name"))

    @Test fun camelCaseKeysPassThrough() {
        val w = WireConfig.toWebConfig(full())
        assertEquals("+919876543210", w.getString("phone"))
        assertEquals("https://img", w.getString("faceMatchReferenceImage"))
        assertEquals("Acme", w.getString("businessName"))
        assertEquals("https://logo", w.getString("logo"))
        assertEquals("https://custom.idto.ai", w.getString("baseUrl"))
        assertTrue(w.has("aadhaarConfig"))
        assertTrue(w.has("nameMatchConfig"))
        assertTrue(w.has("faceMatchConfig"))
        assertTrue(w.has("panConfig"))
        assertTrue(w.has("colors"))
    }

    @Test fun minimalConfigEmitsOnlyThreeKeys() {
        val w = WireConfig.toWebConfig(minimal())
        assertEquals(3, w.length())
        assertTrue(w.has("client_token"))
        assertTrue(w.has("workflow_template_id"))
        assertEquals("full_screen", w.getString("displayMode"))
    }

    @Test fun displayModeForcedFullScreenEvenWhenBottomSheet() =
        assertEquals("full_screen", WireConfig.toWebConfig(full()).getString("displayMode"))

    @Test fun bottomSheetNeverSerialized() =
        assertFalse(WireConfig.toWebConfig(full()).has("bottomSheet"))

    @Test fun nativeOnlyKeysNeverSerialized() {
        val w = WireConfig.toWebConfig(full())
        assertFalse(w.has("debug"))
        assertFalse(w.has("readyTimeoutMs"))
        assertFalse(w.has("allowedHosts"))
    }

    @Test fun envWireValues() {
        assertEquals("development", WireConfig.toWebConfig(full()).getString("env"))
        val prod = IDtoConfig.Builder("ct", "wf").env(IDtoEnv.PRODUCTION).build()
        assertEquals("production", WireConfig.toWebConfig(prod).getString("env"))
    }

    @Test fun themeWireValues() {
        assertEquals("dark", WireConfig.toWebConfig(full()).getString("theme"))
        val light = IDtoConfig.Builder("ct", "wf").theme(IDtoTheme.LIGHT).build()
        assertEquals("light", WireConfig.toWebConfig(light).getString("theme"))
    }

    @Test fun languageWireValues() {
        assertEquals("hi", WireConfig.toWebConfig(full()).getString("language"))
        val en = IDtoConfig.Builder("ct", "wf").language(IDtoLanguage.EN).build()
        assertEquals("en", WireConfig.toWebConfig(en).getString("language"))
    }

    @Test fun aadhaarNestedWireKeys() {
        val a = WireConfig.toWebConfig(full()).getJSONObject("aadhaarConfig")
        assertEquals(1, a.getInt("digilockerMaxFailures"))
        assertTrue(a.getBoolean("okycEnabled"))
    }

    @Test fun faceMatchNestedWireKeys() {
        val f = WireConfig.toWebConfig(full()).getJSONObject("faceMatchConfig")
        assertFalse(f.getBoolean("skipLiveness"))
        assertEquals(75, f.getInt("threshold"))
        assertEquals("fail_closed", f.getString("livenessFailurePolicy"))
    }

    @Test fun livenessPolicyAllWireValues() {
        fun policy(p: IDtoLivenessPolicy) = WireConfig.toWebConfig(
            IDtoConfig.Builder("ct", "wf")
                .faceMatchConfig(IDtoFaceMatchConfig.Builder().livenessFailurePolicy(p).build()).build()
        ).getJSONObject("faceMatchConfig").getString("livenessFailurePolicy")
        assertEquals("fail_open", policy(IDtoLivenessPolicy.FAIL_OPEN))
        assertEquals("fail_closed", policy(IDtoLivenessPolicy.FAIL_CLOSED))
        assertEquals("needs_review", policy(IDtoLivenessPolicy.NEEDS_REVIEW))
    }

    @Test fun panNestedWireKey() =
        assertTrue(WireConfig.toWebConfig(full()).getJSONObject("panConfig").getBoolean("skipContextScreen"))

    @Test fun nameMatchNestedWireKeys() {
        val n = WireConfig.toWebConfig(full()).getJSONObject("nameMatchConfig")
        assertEquals(80, n.getInt("threshold"))
        assertEquals("any", n.getString("decision_mode"))
        assertEquals("late", n.getJSONArray("extra_honorifics").getString(0))
        assertEquals("mohammed", n.getJSONObject("aliases").getString("mohammad"))
        val pairs = n.getJSONArray("compare_pairs")
        val set = (0 until pairs.length()).map { pairs.getString(it) }.toSet()
        assertEquals(setOf("aadhaar_vs_pan", "aadhaar_vs_dl", "aadhaar_vs_bank", "pan_vs_bank"), set)
    }

    @Test fun decisionModeAllWireValue() {
        val all = WireConfig.toWebConfig(
            IDtoConfig.Builder("ct", "wf")
                .nameMatchConfig(IDtoNameMatchConfig.Builder().decisionMode(IDtoDecisionMode.ALL).build()).build()
        ).getJSONObject("nameMatchConfig").getString("decision_mode")
        assertEquals("all", all)
    }

    @Test fun colorsAllEightWireKeys() {
        val c = WireConfig.toWebConfig(full()).getJSONObject("colors")
        assertEquals("#000", c.getString("background"))
        assertEquals("#111", c.getString("text"))
        assertEquals("#222", c.getString("text2"))
        assertEquals("#333", c.getString("border"))
        assertEquals("#444", c.getString("primary"))
        assertEquals("#555", c.getString("secondary"))
        assertEquals("#666", c.getString("buttonTextColor_primary"))
        assertEquals("#777", c.getString("buttonTextColor_secondary"))
    }

    @Test fun buildConfigInjectionExactShape() {
        val injection = WireConfig.buildConfigInjection(minimal())
        assertTrue(injection.startsWith("window.__IDTO_ANDROID_CONFIG__ = "))
        assertTrue(injection.contains("; window.__IDTO_ANDROID_DEBUG__ = false;"))
        val json = injection
            .removePrefix("window.__IDTO_ANDROID_CONFIG__ = ")
            .substringBefore("; window.__IDTO_ANDROID_DEBUG__ =")
        assertEquals("ct-123", JSONObject(json).getString("client_token"))
    }

    @Test fun buildConfigInjectionDebugFlagReflectsConfig() {
        val injection = WireConfig.buildConfigInjection(full())
        assertTrue(injection.contains("; window.__IDTO_ANDROID_DEBUG__ = true;"))
    }

    @Test fun htmlSafeNoRawAngleBracketAndRoundTrips() {
        val nasty = "</script><script>alert(1)</script><!--<script>\"q\"\\b  "
        val cfg = IDtoConfig.Builder("ct", "wf").businessName(nasty).build()
        val injection = WireConfig.buildConfigInjection(cfg)
        val json = injection
            .removePrefix("window.__IDTO_ANDROID_CONFIG__ = ")
            .substringBefore("; window.__IDTO_ANDROID_DEBUG__ =")
        assertFalse(json.contains("<"))
        assertEquals(nasty, JSONObject(json).getString("businessName"))
    }
}
