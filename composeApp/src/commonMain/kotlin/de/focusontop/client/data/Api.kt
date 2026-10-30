package de.focusontop.client.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

const val BASE_URL = "https://www.focusontop.de"

val AppJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
}

/** status = HTTP code (0 = no connection); code = the server's `error` field. */
class ApiError(val status: Int, val code: String?, override val message: String?) : Exception(message)

object Media {
    fun abs(path: String?): String? = when {
        path.isNullOrBlank() -> null
        path.startsWith("http") -> path
        else -> BASE_URL + (if (path.startsWith("/")) path else "/$path")
    }

    fun image(path: String?, width: Int = 1280): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http")) return path
        return "$BASE_URL/api/image-preview?w=$width&url=${path.encodeURLParameter()}"
    }

    fun download(path: String?, name: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http")) return path
        return "$BASE_URL/api/public-download?url=${path.encodeURLParameter()}&name=${(name ?: "file").encodeURLParameter()}"
    }

    /** Best playable source: HLS → light preview → original. */
    fun video(f: ClientFile): String? = abs(f.hlsUrl) ?: abs(f.previewUrl) ?: abs(f.url)

    fun contract(token: String) = "$BASE_URL/#/contract/$token"

    /** A link that opens the file in the website's viewer (for sharing / copying). */
    fun share(f: ClientFile): String? {
        val u = abs(f.url) ?: return null
        return "$BASE_URL/#/view-file?url=${u.encodeURLParameter()}&name=${(f.name ?: f.fileName ?: "Datei").encodeURLParameter()}"
    }

    fun web(path: String) = "$BASE_URL/#/$path"
    const val PRIVACY = "$BASE_URL/privacy.html"
}

class Api(private val token: () -> String?) {

    @PublishedApi
    internal val http = HttpClient {
        expectSuccess = false
        install(ContentNegotiation) { json(AppJson) }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
        }
    }

    @PublishedApi
    internal fun HttpRequestBuilder.auth() {
        token()?.let { header("Authorization", "Bearer $it") }
        header("Accept", "application/json")
    }

    @PublishedApi
    internal suspend fun check(r: HttpResponse): HttpResponse {
        if (r.status.isSuccess()) return r
        val text = runCatching { r.bodyAsText() }.getOrNull()
        val obj = runCatching { AppJson.parseToJsonElement(text ?: "").jsonObject }.getOrNull()
        val code = obj?.get("error")?.jsonPrimitive?.content
        val msg = obj?.get("message")?.jsonPrimitive?.content ?: code
        throw ApiError(r.status.value, code, msg)
    }

    @PublishedApi
    internal suspend inline fun <T> net(block: () -> T): T = try {
        block()
    } catch (e: ApiError) {
        throw e
    } catch (e: Exception) {
        throw ApiError(0, "network", e.message)
    }

    suspend inline fun <reified T> get(path: String): T = net {
        check(http.get(BASE_URL + path) { auth() }).body()
    }

    suspend inline fun <reified T> post(path: String, body: JsonObject = JsonObject(emptyMap())): T = net {
        check(http.post(BASE_URL + path) { auth(); contentType(ContentType.Application.Json); setBody(body) }).body()
    }

    suspend fun postNoBody(path: String, body: JsonObject = JsonObject(emptyMap())) = net {
        check(http.post(BASE_URL + path) { auth(); contentType(ContentType.Application.Json); setBody(body) })
        Unit
    }

    suspend fun patchNoBody(path: String, body: JsonObject = JsonObject(emptyMap())) = net {
        check(http.patch(BASE_URL + path) { auth(); contentType(ContentType.Application.Json); setBody(body) })
        Unit
    }

    suspend fun deleteNoBody(path: String) = net {
        check(http.delete(BASE_URL + path) { auth() })
        Unit
    }

    /* ── endpoints ── */

    suspend fun login(email: String, password: String): LoginResponse =
        post("/api/auth/login", buildJsonObject { put("email", email.trim()); put("password", password) })

    suspend fun verify2fa(userId: String, code: String): LoginResponse =
        post("/api/auth/verify-2fa", buildJsonObject { put("userId", userId); put("token", code.trim()) })

    suspend fun register(name: String, email: String, password: String, company: String?, privateAccount: Boolean, phone: String? = null): kotlinx.serialization.json.JsonObject =
        post("/api/auth/register", buildJsonObject {
            put("name", name.trim()); put("email", email.trim()); put("password", password); put("role", "client")
            put("communityPrivate", privateAccount)
            if (!company.isNullOrBlank()) put("company", company.trim())
            if (!phone.isNullOrBlank()) put("phone", phone.trim())
        })

    suspend fun forgotPassword(email: String) =
        postNoBody("/api/auth/forgot-password", buildJsonObject { put("email", email.trim()) })

    suspend fun me(): User = get("/api/auth/me")

    suspend fun clientConfig(): ClientConfig = get("/api/client-app/config")

    /** Google sign-in: the phone's Google ID token is checked by our server. */
    suspend fun loginWithGoogle(googleIdToken: String): LoginResponse =
        post("/api/client-app/google", buildJsonObject { put("idToken", googleIdToken) })

    suspend fun loginWithApple(appleIdToken: String): LoginResponse =
        post("/api/client-app/apple", buildJsonObject { put("idToken", appleIdToken) })

    suspend fun home(): Home = get("/api/client-app/home")

    suspend fun file(id: String): ClientFile = get("/api/client-app/files/$id")
    suspend fun fileVersions(id: String): List<ClientFile> = get("/api/client-app/files/$id/versions")

    suspend fun approve(fileId: String) = postNoBody("/api/files/$fileId/approve")

    suspend fun requestRevision(fileId: String, notes: String) =
        postNoBody("/api/files/$fileId/revision", buildJsonObject { put("notes", notes) })

    suspend fun comments(fileId: String): List<Comment> = get("/api/files/$fileId/comments")

    suspend fun addComment(fileId: String, text: String, timestampSeconds: Double? = null) =
        postNoBody("/api/file-comments/$fileId", buildJsonObject {
            put("text", text)
            if (timestampSeconds != null) put("timestampSeconds", timestampSeconds)
        })

    suspend fun resolveComment(commentId: String, resolved: Boolean) =
        patchNoBody("/api/file-comments/$commentId/resolve", buildJsonObject { put("resolved", resolved) })

    suspend fun notifications(): List<AppNotification> = get("/api/notifications")

    suspend fun markRead(id: String) = patchNoBody("/api/notifications/$id/read")

    suspend fun markAllRead() = patchNoBody("/api/notifications/read-all")

    suspend fun registerPush(token: String, platform: String) = postNoBody(
        "/api/push/register",
        buildJsonObject { put("token", token); put("platform", platform); put("appId", "de.focusontop.client") },
    )

    /** Uses the old bearer explicitly: it runs while the session is being cleared. */
    suspend fun unregisterPush(pushToken: String, bearer: String) = net {
        http.post("$BASE_URL/api/push/unregister") {
            header("Authorization", "Bearer $bearer")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("token", pushToken) })
        }
        Unit
    }

    /** Profile changes go to the team for approval (same flow as the web portal). */
    suspend fun requestProfileUpdate(changes: Map<String, String>) =
        postNoBody("/api/profile/request-update", buildJsonObject { put("changes", buildJsonObject { changes.forEach { (k, v) -> put(k, v) } }) })

    suspend fun requestDelete() = postNoBody("/api/profile/request-delete", buildJsonObject { put("reason", "client app") })

    /* ── client app v2 ── */

    suspend fun clientNotifications(): List<AppNotification> = get("/api/client-app/notifications")

    suspend fun chat(): ChatThread = get("/api/client-app/chat")

    suspend fun sendChat(text: String, fileUrl: String? = null, fileName: String? = null, mimeType: String? = null): ChatMessage =
        post("/api/client-app/chat", buildJsonObject {
            put("text", text)
            if (fileUrl != null) { put("fileUrl", fileUrl); put("fileName", fileName ?: ""); put("mimeType", mimeType ?: "") }
        })

    suspend fun availability(date: String, minutes: Int): Availability =
        get("/api/client-app/availability?date=$date&duration=$minutes")

    suspend fun requestAppointment(type: String, date: String, time: String, note: String) =
        postNoBody("/api/client-app/appointment-requests", buildJsonObject {
            put("type", type); put("date", date); put("time", time); put("note", note)
        })

    /* Chunked, resumable upload — the same endpoints the website uses. */
    suspend fun uploadInit(uploadId: String, filename: String, size: Long): UploadInit =
        post("/api/upload/init", buildJsonObject { put("uploadId", uploadId); put("filename", filename); put("size", size) })

    /** Sends one chunk; returns the server's byte count afterwards (or the count it expects on 409). */
    suspend fun uploadChunk(uploadId: String, offset: Long, bytes: ByteArray): Long = net {
        val r = http.post("$BASE_URL/api/upload/chunk?uploadId=$uploadId") {
            auth()
            header("x-chunk-offset", offset.toString())
            contentType(ContentType.Application.OctetStream)
            setBody(bytes)
        }
        when (r.status.value) {
            in 200..299 -> runCatching { AppJson.parseToJsonElement(r.bodyAsText()).jsonObject["received"]?.jsonPrimitive?.content?.toLong() }.getOrNull() ?: (offset + bytes.size)
            409 -> runCatching { AppJson.parseToJsonElement(r.bodyAsText()).jsonObject["received"]?.jsonPrimitive?.content?.toLong() }.getOrNull() ?: offset
            429 -> -1L
            else -> { check(r); offset }
        }
    }

    suspend fun uploadComplete(uploadId: String, filename: String, size: Long, mimeType: String): UploadedBlob =
        post("/api/upload/complete", buildJsonObject {
            put("uploadId", uploadId); put("filename", filename); put("size", size); put("mimeType", mimeType)
        })

    suspend fun registerUpload(b: UploadedBlob, filename: String, folder: String?, quiet: Boolean): ClientFile =
        post("/api/client-app/uploads", buildJsonObject {
            put("url", b.url ?: ""); put("filename", filename); put("byteSize", b.byteSize ?: 0L)
            put("mimeType", b.mimeType ?: ""); put("checksum", b.checksum ?: "")
            if (folder != null) put("folder", folder)
            put("quiet", quiet)
        })
    /* ── community ── */
    suspend fun feed(discover: Boolean): List<CPost> = get("/api/client-app/community/feed?scope=${if (discover) "discover" else "following"}")
    suspend fun stories(): List<CStoryGroup> = get("/api/client-app/community/stories")
    suspend fun storySeen(id: String) = postNoBody("/api/client-app/community/stories/$id/seen")
    suspend fun communityPost(id: String): CPost = get("/api/client-app/community/posts/$id")
    suspend fun publish(kind: String, mediaUrl: String, mediaType: String, caption: String, tags: List<String>, public: Boolean): CPost =
        post("/api/client-app/community/posts", buildJsonObject {
            put("kind", kind); put("mediaUrl", mediaUrl); put("mediaType", mediaType); put("caption", caption)
            put("visibility", if (public) "public" else "account")
            put("tags", kotlinx.serialization.json.JsonArray(tags.map { kotlinx.serialization.json.JsonPrimitive(it) }))
        })
    suspend fun deletePost(id: String) = deleteNoBody("/api/client-app/community/posts/$id")
    suspend fun like(id: String): CPost = post("/api/client-app/community/posts/$id/like", JsonObject(emptyMap()))
    suspend fun postComments(id: String): List<CComment> = get("/api/client-app/community/posts/$id/comments")
    suspend fun addPostComment(id: String, text: String): CComment =
        post("/api/client-app/community/posts/$id/comments", buildJsonObject { put("text", text) })
    suspend fun deletePostComment(id: String) = deleteNoBody("/api/client-app/community/comments/$id")
    suspend fun profile(id: String): CProfile = get("/api/client-app/community/profile/$id")
    suspend fun follow(id: String): FollowState = post("/api/client-app/community/follow/$id", JsonObject(emptyMap()))
    suspend fun people(q: String): List<CPerson> = get("/api/client-app/community/people?q=${q.encodeURLParameter()}")
    suspend fun communityMe(): CMe = get("/api/client-app/community/me")
    suspend fun setPrivate(private: Boolean) =
        patchNoBody("/api/client-app/community/me", buildJsonObject { put("private", private) })
}
