package ai.idto.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class ParityTest {

    private val configInterfaces = listOf(
        "IDtoColors",
        "IDtoAadhaarConfig",
        "IDtoFaceMatchConfig",
        "IDtoPanConfig",
        "IDtoNameMatchConfig",
        "IDtoWidgetConfig",
    )

    private val notRepresented = setOf(
        "onStepComplete", "onWorkflowComplete", "onAbandon", "onError", "onClose",
        "desktopModal", "breakpoint",
        "getToken", "width", "height",
    )

    private fun locateContract(): File? {
        System.getenv("IDTO_WEB_SDK_DTS")?.let { return File(it) }
        val candidate = File("../../web_sdk/src/cdn/global.d.ts")
        return if (candidate.exists()) candidate else null
    }

    private fun stripComments(src: String): String =
        src.replace(Regex("/\\*[\\s\\S]*?\\*/"), "").replace(Regex("//[^\n]*"), "")

    private fun interfaceBody(src: String, name: String): String {
        val start = Regex("interface\\s+$name\\b").find(src)?.range?.first ?: return ""
        val open = src.indexOf('{', start)
        if (open < 0) return ""
        var depth = 0
        for (i in open until src.length) {
            when (src[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return src.substring(open + 1, i)
                }
            }
        }
        return ""
    }

    private fun propertyNames(body: String): Set<String> =
        Regex("(?m)^\\s*([A-Za-z_]\\w*)\\s*\\??\\s*:").findAll(body).map { it.groupValues[1] }.toSet()

    private fun stringLiterals(src: String): Set<String> =
        Regex("'([^']+)'").findAll(src).map { it.groupValues[1] }.toSet()

    private fun present(token: String, haystack: String): Boolean =
        Regex("\\b${Regex.escape(token)}\\b").containsMatchIn(haystack)

    @Test fun webConfigContractFullyMirrored() {
        val contract = locateContract()
        assumeTrue(contract != null && contract.exists())

        val dts = stripComments(contract!!.readText())
        val surface = stripComments(
            listOf(
                "src/main/kotlin/ai/idto/sdk/IDtoConfig.kt",
                "src/main/kotlin/ai/idto/sdk/IDtoModuleConfigs.kt",
                "src/main/kotlin/ai/idto/sdk/internal/WireConfig.kt",
            ).joinToString("\n") { File(it).readText() }
        )

        val expectedFields = mutableSetOf<String>()
        for (iface in configInterfaces) {
            val body = interfaceBody(dts, iface)
            assertTrue("interface $iface not found in contract", body.isNotEmpty())
            expectedFields += propertyNames(body)
        }
        expectedFields -= notRepresented

        assertTrue("parser sanity floor: ${expectedFields.size}", expectedFields.size > 20)

        val expectedEnums = stringLiterals(dts)
        val missingFields = expectedFields.filterNot { present(it, surface) }.sorted()
        val missingEnums = expectedEnums.filterNot { present(it, surface) }.sorted()

        assertEquals("missing config fields", emptyList<String>(), missingFields)
        assertEquals("missing enum members", emptyList<String>(), missingEnums)
    }
}
