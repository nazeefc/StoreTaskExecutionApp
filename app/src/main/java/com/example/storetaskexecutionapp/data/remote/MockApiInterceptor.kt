package com.example.storetaskexecutionapp.data.remote

import com.google.gson.Gson
import com.google.gson.JsonParser
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Serves a mock REST API over Retrofit so the app works without a backend.
 * Requests still fail when the device is offline, which exercises the real
 * offline / retry / WorkManager paths.
 */
class MockApiInterceptor(
    private val isOnline: () -> Boolean
) : Interceptor {

    private val gson = Gson()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!isOnline()) {
            throw java.net.UnknownHostException("No internet connection")
        }

        Thread.sleep(450)

        val request = chain.request()
        val path = request.url.encodedPath

        return when {
            request.method == "GET" && path.matches(TASKS_PATH) -> {
                val employeeId = path.split("/").getOrNull(2).orEmpty()
                when (employeeId.uppercase()) {
                    "FAIL" -> json(request, 500, """{"message":"Unable to load assigned tasks"}""")
                    "EMPTY" -> json(request, 200, """{"tasks":[]}""")
                    else -> json(request, 200, gson.toJson(mapOf("tasks" to mockTasks(employeeId))))
                }
            }

            request.method == "POST" && path == "/tasks/sync" -> {
                val body = request.body ?: return json(request, 400, """{"message":"Missing body"}""")
                val buffer = okio.Buffer()
                body.writeTo(buffer)
                val parsed = JsonParser.parseString(buffer.readUtf8()).asJsonObject
                val ids = parsed.getAsJsonArray("tasks")?.mapNotNull { item ->
                    item.asJsonObject.get("id")?.asString
                }.orEmpty()
                json(request, 200, gson.toJson(mapOf("syncedIds" to ids)))
            }

            else -> json(request, 404, """{"message":"Not found"}""")
        }
    }

    private fun json(request: okhttp3.Request, code: Int, body: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code in 200..299) "OK" else "Error")
            .body(body.toResponseBody(jsonMedia))
            .build()

    private fun mockTasks(employeeId: String): List<Map<String, String>> = listOf(
        task(employeeId, "01", "Restock beverage cooler", "Refill water, soda, and energy drinks. Rotate older stock forward.", "Downtown SuperMart", "Aisle 4", "PENDING", "2026-10-22"),
        task(employeeId, "02", "Snacks planogram check", "Confirm shelf facings match the weekly planogram and fill gaps.", "Downtown SuperMart", "Aisle 7", "PENDING", "2026-10-22"),
        task(employeeId, "03", "Spill cleanup verification", "Inspect produce wet-spill zone and photograph after sanitizing.", "Downtown SuperMart", "Produce", "PENDING", "2026-10-23"),
        task(employeeId, "04", "Price tag audit", "Replace missing or incorrect shelf labels on promotional endcaps.", "Downtown SuperMart", "Front endcaps", "PENDING", "2026-10-23"),
        task(employeeId, "05", "Backroom expiry check", "Pull expired dairy from the walk-in and log discarded units.", "Downtown SuperMart", "Backroom cooler", "PENDING", "2026-10-24")
    )

    private fun task(
        employeeId: String,
        suffix: String,
        title: String,
        description: String,
        store: String,
        aisle: String,
        status: String,
        due: String
    ) = mapOf(
        "id" to "$employeeId-T$suffix",
        "title" to title,
        "description" to description,
        "storeName" to store,
        "aisle" to aisle,
        "status" to status,
        "dueDate" to due
    )

    companion object {
        private val TASKS_PATH = Regex("^/employees/[^/]+/tasks$")
    }
}
