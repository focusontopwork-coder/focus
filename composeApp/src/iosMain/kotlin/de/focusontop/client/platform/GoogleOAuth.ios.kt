package de.focusontop.client.platform

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.CoreCrypto.CC_SHA256
import platform.Foundation.*
import platform.Security.SecRandomCopyBytes
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import kotlin.coroutines.resume

/**
 * Google sign-in on iPhone without any external SDK:
 * ASWebAuthenticationSession + OAuth authorization-code flow with PKCE.
 * Returns the Google ID token — the same token shape the Android app gets
 * from the Credential Manager, so the server sees one identical flow.
 *
 * The client ID must be an "iOS" OAuth client of the same Google/Firebase
 * project (the server hands it over as googleIosClientId). Using a web-only
 * client would fail the token exchange, because web clients need a secret.
 */
object GoogleOAuth {

    private val http by lazy { HttpClient() }
    private var activeSession: ASWebAuthenticationSession? = null
    private var activeProvider: NSObject? = null

    suspend fun idToken(clientId: String): String? = withContext(Dispatchers.Main) {
        val scheme = clientId.split('.').reversed().joinToString(".")
        val redirect = "$scheme:/oauthredirect"
        val verifier = randomBase64Url(48)
        val challenge = sha256Base64Url(verifier.encodeToByteArray())
        val authUrl = "https://accounts.google.com/o/oauth2/v2/auth" +
            "?client_id=${enc(clientId)}" +
            "&redirect_uri=${enc(redirect)}" +
            "&response_type=code" +
            "&scope=${enc("openid email profile")}" +
            "&code_challenge=$challenge" +
            "&code_challenge_method=S256" +
            "&prompt=select_account"
        val code = present(authUrl, scheme) ?: return@withContext null
        exchange(clientId, code, verifier, redirect)
    }

    /** Percent-encodes a value for a URL query string / x-www-form-urlencoded
     *  body (RFC 3986 unreserved set). Not base64 — base64 is for the PKCE
     *  verifier/challenge below; using it here was sending Google a garbled
     *  client_id/redirect_uri, which is why the auth page returned "400". */
    private fun enc(v: String): String {
        val unreserved = ('A'..'Z') + ('a'..'z') + ('0'..'9') + listOf('-', '_', '.', '~')
        val sb = StringBuilder()
        for (b in v.encodeToByteArray()) {
            val c = (b.toInt() and 0xFF).toChar()
            if (c.code < 128 && c in unreserved) sb.append(c)
            else sb.append('%').append((b.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0'))
        }
        return sb.toString()
    }

    /** Opens the Google account sheet; null when the user closes it. */
    private suspend fun present(url: String, scheme: String): String? =
        suspendCancellableCoroutine { cont ->
            val nsUrl = NSURL.URLWithString(url)
            if (nsUrl == null) {
                cont.resume(null, null)
                return@suspendCancellableCoroutine
            }
            val session = ASWebAuthenticationSession(
                uRL = nsUrl,
                callbackURLScheme = scheme,
            ) { callback, _ ->
                // the callback comes back as "scheme://?code=…" — parse it as text
                val code = callback?.query
                    ?.split('&')
                    ?.firstOrNull { it.substringBefore('=') == "code" }
                    ?.substringAfter('=', "")
                    ?.let { runCatching { NSString.create(string = it).stringByRemovingPercentEncoding }.getOrNull() ?: it }
                if (cont.isActive) cont.resume(code, null)
            }
            val provider = object : NSObject(),
                ASWebAuthenticationPresentationContextProvidingProtocol {
                override fun presentationAnchorForWebAuthenticationSession(
                    session: ASWebAuthenticationSession,
                ): UIWindow = UIApplication.sharedApplication.keyWindow ?: UIWindow()
            }
            session.presentationContextProvider = provider
            activeSession = session
            activeProvider = provider
            session.start()
        }

    /** Exchanges the authorization code for tokens and returns the ID token. */
    private suspend fun exchange(clientId: String, code: String, verifier: String, redirect: String): String? =
        try {
            val body = "client_id=${enc(clientId)}&code=${enc(code)}" +
                "&code_verifier=${enc(verifier)}&grant_type=authorization_code" +
                "&redirect_uri=${enc(redirect)}"
            val text = http.post("https://oauth2.googleapis.com/token") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(body)
            }.bodyAsText()
            Json.parseToJsonElement(text).jsonObject["id_token"]?.jsonPrimitive?.content
        } catch (_: Exception) {
            null
        }

    @OptIn(ExperimentalForeignApi::class)
    private fun randomBase64Url(count: Int): String {
        val buf = ByteArray(count)
        buf.usePinned { p -> SecRandomCopyBytes(null, count.toULong(), p.addressOf(0).reinterpret<kotlinx.cinterop.UByteVar>()) }
        return buf.toNSData().base64UrlSafe()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun sha256Base64Url(data: ByteArray): String {
        val out = ByteArray(32)
        data.usePinned { src -> out.usePinned { dst ->
            CC_SHA256(
                src.addressOf(0).reinterpret<kotlinx.cinterop.UByteVar>(),
                data.size.toUInt(),
                dst.addressOf(0).reinterpret<kotlinx.cinterop.UByteVar>(),
            )
        } }
        return out.toNSData().base64UrlSafe()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun ByteArray.toNSData(): NSData = usePinned { p ->
        NSData.create(bytes = p.addressOf(0), length = size.toULong())
    } ?: NSData()

    private fun NSData.base64UrlSafe(): String =
        base64EncodedStringWithOptions(0u)
            .replace("+", "-")
            .replace("/", "_")
            .replace("=", "")
}
