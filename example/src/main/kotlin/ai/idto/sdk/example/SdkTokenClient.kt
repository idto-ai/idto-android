package ai.idto.sdk.example

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class SdkTokenClient(private val baseUrl: String) {

    fun fetchClientToken(clientId: String, clientSecret: String): String {
        val connection = URL("$baseUrl/auth/sdk/token").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("Content-Type", "application/json")
            val body = JSONObject()
                .put("client_id", clientId)
                .put("client_secret", clientSecret)
                .toString()
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw IOException("token request failed ($code): $text")
            }
            val token = JSONObject(text).optString("access_token", "")
            if (token.isEmpty()) throw IOException("no access_token in response")
            return token
        } finally {
            connection.disconnect()
        }
    }
}
