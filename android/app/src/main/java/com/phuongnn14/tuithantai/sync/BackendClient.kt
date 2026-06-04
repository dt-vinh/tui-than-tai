package com.phuongnn14.tuithantai.sync

import com.phuongnn14.tuithantai.data.ExpenseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AuthResult(
    val accessToken: String,
    val refreshToken: String,
    val email: String
)

class BackendClient(
    private val baseUrl: String
) {
    suspend fun register(email: String, password: String, name: String): AuthResult {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .put("name", name)
        return parseAuth(post("/auth/register", body))
    }

    suspend fun login(email: String, password: String): AuthResult {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
        return parseAuth(post("/auth/login", body))
    }

    suspend fun pushExpenses(token: String, expenses: List<ExpenseEntity>): Map<String, Long> {
        if (expenses.isEmpty()) return emptyMap()
        val array = JSONArray()
        expenses.forEach { expense ->
            array.put(
                JSONObject()
                    .put("id", expense.id)
                    .put("title", expense.title)
                    .put("amount", expense.amount)
                    .put("currency", expense.currency)
                    .put("categoryId", expense.categoryId)
                    .put("wallet", expense.wallet)
                    .put("note", expense.note)
                    .put("receiptPath", expense.receiptPath)
                    .put("ocrText", expense.ocrText)
                    .put("spentAt", expense.spentAt)
                    .put("updatedAt", expense.updatedAt)
                    .put("deletedAt", expense.deletedAt ?: JSONObject.NULL)
            )
        }
        val response = post("/sync/push", JSONObject().put("expenses", array), token)
        val saved = response.getJSONArray("expenses")
        val result = mutableMapOf<String, Long>()
        for (i in 0 until saved.length()) {
            val item = saved.getJSONObject(i)
            result[item.getString("id")] = item.optLong("serverVersion", 0)
        }
        return result
    }

    private fun parseAuth(json: JSONObject): AuthResult {
        val user = json.getJSONObject("user")
        return AuthResult(
            accessToken = json.getString("accessToken"),
            refreshToken = json.getString("refreshToken"),
            email = user.getString("email")
        )
    }

    private suspend fun post(path: String, body: JSONObject, token: String = ""): JSONObject = withContext(Dispatchers.IO) {
        val connection = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection)
        connection.requestMethod = "POST"
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        if (token.isNotBlank()) connection.setRequestProperty("Authorization", "Bearer $token")
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream.bufferedReader().use { it.readText() }
        if (code !in 200..299) error("HTTP $code: $text")
        JSONObject(text)
    }
}
