package de.focusontop.client.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.focusontop.client.i18n.Lang
import de.focusontop.client.platform.Prefs
import de.focusontop.client.platform.PushBridge
import de.focusontop.client.platform.platformName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import de.focusontop.client.platform.PickedFile
import kotlin.random.Random

private const val CHUNK = 4 * 1024 * 1024

sealed class Screen {
    data object Splash : Screen()
    data object Welcome : Screen()
    data object Onboarding : Screen()
    data object PrivacyConsent : Screen()
    data object NotificationsPermission : Screen()
    data object Login : Screen()
    data object Forgot : Screen()
    data object Register : Screen()
    data object Main : Screen()
    data object Notifications : Screen()
    data class FileDetail(val file: ClientFile) : Screen()
    data class CompareVersions(val file: ClientFile) : Screen()
    /** Full-screen viewer: swipe through [ids], starting at [index]. */
    data class Viewer(val ids: List<String>, val index: Int = 0, val title: String? = null) : Screen()
    data object Messages : Screen()
    data class Chat(val draft: String = "") : Screen()
    data object AppointmentRequest : Screen()
    data object Settings : Screen()
    data object EditProfile : Screen()
    data object Legal : Screen()
    data object Plans : Screen()
    data object NewProject : Screen()
    data object PlanCompare : Screen()
    data class ProjectDetail(val name: String) : Screen()
    data class Delivery(val name: String) : Screen()
    data object Files : Screen()
    data object Invoices : Screen()
    data object Projects : Screen()
    /** Stories of all groups, starting at [group]. */
    data class Stories(val groups: List<CStoryGroup>, val group: Int) : Screen()
    data class Compose(val kind: String) : Screen()          // post | story
    data class Post(val id: String, val post: CPost? = null) : Screen()
    data class ClientProfile(val id: String) : Screen()
    data object Discover : Screen()
    data object StoryCamera : Screen()
}

enum class Tab { HOME, SEARCH, FILES, CALENDAR, PROFILE }

/** One file on its way to the server. */
data class UploadJob(val name: String, val size: Long, val sent: Long = 0, val done: Boolean = false, val failed: Boolean = false)

/** The whole app state in one place: session, language, navigation, data. */
class AppState {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var token by mutableStateOf(Prefs.get("token"))
        private set
    var lang by mutableStateOf(Lang.from(Prefs.get("lang") ?: defaultLang()))
        private set
    var user by mutableStateOf<User?>(null)
        private set

    val api = Api { token }

    // navigation: a simple back stack
    val stack = mutableStateListOf<Screen>(Screen.Splash)
    val current: Screen get() = stack.last()
    var tab by mutableStateOf(Tab.HOME)

    // data
    var home by mutableStateOf<Home?>(null)
        private set
    var homeError by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(false)
        private set

    // Face ID / biometric opt-in (Figma "03b · Zugang · Face ID")
    var faceIdPromptPending by mutableStateOf(false)
        private set
    var faceIdLocked by mutableStateOf(false)
        private set
    val faceIdEnabled: Boolean get() = Prefs.get("faceid_enabled") == "1"
    fun enableFaceId() { Prefs.set("faceid_enabled", "1"); Prefs.set("faceid_asked", "1"); faceIdPromptPending = false }
    fun dismissFaceIdPrompt() { Prefs.set("faceid_asked", "1"); faceIdPromptPending = false }
    fun unlockFaceId() { faceIdLocked = false }
    private fun maybeOfferFaceId() {
        if (Prefs.get("faceid_asked") != null) return
        scope.launch {
            if (de.focusontop.client.platform.biometricAvailable()) faceIdPromptPending = true
        }
    }

    fun go(screen: Screen) { stack.add(screen) }
    fun replaceAll(screen: Screen) { stack.clear(); stack.add(screen) }
    fun back(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun setLanguage(l: Lang) {
        lang = l
        Prefs.set("lang", l.code)
    }

    val seenWelcome: Boolean get() = Prefs.get("welcomed") == "1"
    fun markWelcomed() = Prefs.set("welcomed", "1")

    init {
        // Whenever a push token shows up while signed in, send it to the server.
        scope.launch {
            PushBridge.token.filterNotNull().collect { t -> if (token != null) registerPush(t) }
        }
    }

    /** Splash: restore the session if the saved token still works. */
    fun start() {
        scope.launch {
            val t = token
            if (t == null) {
                replaceAll(if (seenWelcome) Screen.Login else Screen.Welcome)
                return@launch
            }
            try {
                val me = api.me()
                if (me.role != "client") { signOut(); return@launch }
                user = me
                replaceAll(Screen.Main)
                if (faceIdEnabled) {
                    faceIdLocked = true
                    scope.launch {
                        de.focusontop.client.platform.biometricAuthenticate(biometricPromptReason())
                        faceIdLocked = false // proceed regardless of result — never lock the user out
                    }
                }
                refresh()
                PushBridge.token.value?.let { registerPush(it) }
            } catch (e: ApiError) {
                if (e.status == 401 || e.status == 403) signOut()
                else { // offline: keep the session, show what we can
                    replaceAll(Screen.Main)
                    homeError = e.message
                }
            }
        }
    }

    /** Called after a successful login (or 2FA). Refuses staff accounts. */
    fun onLoggedIn(resp: LoginResponse): Boolean {
        val u = resp.user
        val t = resp.token ?: return false
        if (u?.role != "client") return false
        token = t
        Prefs.set("token", t)
        user = u
        markWelcomed()
        replaceAll(Screen.Main)
        tab = Tab.HOME
        maybeOfferFaceId()
        refresh()
        PushBridge.token.value?.let { registerPush(it) }
        return true
    }

    private fun biometricPromptReason(): String = when (lang) {
        Lang.AR -> "تسجيل الدخول إلى Focus On Top"
        Lang.DE -> "Bei Focus On Top anmelden"
        else -> "Sign in to Focus On Top"
    }

    fun refresh() {
        if (token == null) return
        scope.launch {
            loading = true
            try {
                home = api.home()
                homeError = null
                refreshCommunity()
                home?.user?.let { u -> user = (user ?: u).copy(name = u.name, email = u.email, avatar = u.avatar, phone = u.phone, company = u.company) }
            } catch (e: ApiError) {
                if (e.status == 401) signOut() else homeError = e.message
            } finally {
                loading = false
            }
        }
    }

    /** Local, optimistic update after approve / revision. */
    fun updateFile(id: String, status: String, notes: String? = null) {
        val h = home ?: return
        fun patch(f: ClientFile) = if (f.id == id) f.copy(status = status, revisionNotes = notes ?: f.revisionNotes) else f
        home = h.copy(
            pendingReview = h.pendingReview.filter { it.id != id || status == "ready_for_review" },
            files = h.files.map(::patch),
        )
    }

    fun clearUnread() { home = home?.copy(unreadNotifications = 0) }

    // ── community ──
    var stories by mutableStateOf<List<CStoryGroup>>(emptyList())
        private set
    var feed by mutableStateOf<List<CPost>?>(null)
        private set
    var discover by mutableStateOf(true)      // "Für dich" first
        private set
    var reelsMode by mutableStateOf(true)     // FILES tab opens as the reels browser (grid via toggle)
    var me by mutableStateOf<CMe?>(null)
        private set

    fun toggleFeedScope() {
        discover = !discover
        feed = null
        scope.launch { runCatching { api.feed(discover) }.onSuccess { feed = it } }
    }

    fun refreshCommunity() {
        if (token == null) return
        scope.launch {
            runCatching { api.stories() }.onSuccess { stories = it }
            runCatching { api.feed(discover) }.onSuccess { feed = it }
            runCatching { api.communityMe() }.onSuccess { me = it }
        }
    }

    fun setFeedScope(d: Boolean) {
        if (d == discover) return
        discover = d; feed = null
        scope.launch { runCatching { api.feed(d) }.onSuccess { feed = it } }
    }

    /** Optimistic like toggle everywhere the post is shown. */
    fun toggleLike(p: CPost) {
        val flipped = p.copy(liked = !p.liked, likes = (p.likes + if (p.liked) -1 else 1).coerceAtLeast(0))
        replacePost(flipped)
        scope.launch { runCatching { api.like(p.id) }.onSuccess { replacePost(it) }.onFailure { replacePost(p) } }
    }

    fun toggleSave(p: CPost) {
        val flipped = p.copy(saved = !p.saved)
        replacePost(flipped)
        scope.launch { runCatching { api.save(p.id) }.onSuccess { replacePost(it) }.onFailure { replacePost(p) } }
    }

    fun replacePost(p: CPost) {
        feed = feed?.map { if (it.id == p.id) p else it }
        me = me?.let { m -> m.copy(posts = m.posts.map { if (it.id == p.id) p else it }) }
    }

    fun removePost(id: String) {
        feed = feed?.filter { it.id != id }
        me = me?.let { m -> m.copy(posts = m.posts.filter { it.id != id }) }
        stories = stories.map { g -> g.copy(items = g.items.filter { it.id != id }) }.filter { it.items.isNotEmpty() }
    }

    fun markStorySeen(id: String) {
        stories = stories.map { g -> g.copy(items = g.items.map { if (it.id == id) it.copy(seen = true) else it }).let { it.copy(allSeen = it.items.all { i -> i.seen }) } }
        scope.launch { runCatching { api.storySeen(id) } }
    }

    fun setPrivate(p: Boolean) {
        me = me?.copy(private = p) ?: CMe(private = p)
        scope.launch { runCatching { api.setPrivate(p) } }
    }
    /** Short message at the bottom of the screen ("Link copied"). */
    var toast by mutableStateOf<String?>(null)
        private set
    private var toastJob: kotlinx.coroutines.Job? = null
    fun showToast(msg: String) {
        toast = msg
        toastJob?.cancel()
        toastJob = scope.launch { delay(2200); toast = null }
    }

    fun clearUnreadMessages() { home = home?.copy(unreadMessages = 0) }

    /** Find a file the client can see (home list), for the viewer. */
    fun fileById(id: String): ClientFile? = home?.files?.firstOrNull { it.id == id } ?: home?.pendingReview?.firstOrNull { it.id == id }

    // ── favourites (the heart / bookmark): kept on the phone ──
    var favourites by mutableStateOf(Prefs.get("favs")?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet())
        private set
    fun toggleFavourite(id: String) {
        favourites = if (id in favourites) favourites - id else favourites + id
        Prefs.set("favs", favourites.joinToString(","))
    }

    // ── uploads from the phone (files or a whole folder) ──
    val uploads = mutableStateListOf<UploadJob>()
    var uploadMessage by mutableStateOf<String?>(null)
    val uploading: Boolean get() = uploads.any { !it.done && !it.failed }

    fun upload(files: List<PickedFile>) {
        if (files.isEmpty() || token == null) return
        val start = uploads.size
        files.forEach { uploads.add(UploadJob(it.name, it.size)) }
        scope.launch {
            var ok = 0
            files.forEachIndexed { i, f ->
                val idx = start + i
                val quiet = i < files.lastIndex          // one notification for the team per batch
                val result = runCatching {
                    withContext(Dispatchers.Default) { sendOne(f, quiet) { sent -> scope.launch { if (idx < uploads.size) uploads[idx] = uploads[idx].copy(sent = sent) } } }
                }
                uploads[idx] = uploads[idx].copy(done = result.isSuccess, failed = result.isFailure, sent = if (result.isSuccess) f.size else uploads[idx].sent)
                if (result.isSuccess) ok++
            }
            uploadMessage = "$ok/${files.size}"
            refresh()
            delay(4000)
            if (!uploading) { uploads.clear(); uploadMessage = null }
        }
    }

    private suspend fun sendOne(f: PickedFile, quiet: Boolean, progress: (Long) -> Unit) {
        val blob = uploadBlob(f, progress)
        api.registerUpload(blob, f.name, f.folder, quiet)
    }

    /** Chunked upload of one picked file; returns where it landed on the server. */
    suspend fun uploadBlob(f: PickedFile, progress: (Long) -> Unit = {}): UploadedBlob = withContext(Dispatchers.Default) { uploadBlobNow(f, progress) }

    private suspend fun uploadBlobNow(f: PickedFile, progress: (Long) -> Unit): UploadedBlob {
        val id = (1..32).map { "0123456789abcdef"[Random.nextInt(16)] }.joinToString("")
        var offset = api.uploadInit(id, f.name, f.size).received
        val reader = f.open()
        try {
            if (offset > 0) { var skip = offset; while (skip > 0) { val b = reader.read(minOf(skip, CHUNK.toLong()).toInt()) ?: break; skip -= b.size } }
            while (offset < f.size) {
                val bytes = reader.read(CHUNK) ?: break
                var tries = 0
                while (true) {
                    val attempt = runCatching { api.uploadChunk(id, offset, bytes) }
                    if (attempt.isFailure) {
                        if (++tries > 4) throw attempt.exceptionOrNull()!!
                        delay(2000L * tries)
                        continue
                    }
                    val next = attempt.getOrThrow()
                    if (next == -1L) { delay(3000); continue }          // server busy: wait and resend
                    if (next == offset + bytes.size) { offset = next; break }
                    if (++tries > 4) throw ApiError(409, "offset", "upload offset")
                    delay(1000)
                }
                progress(offset)
            }
        } finally { reader.close() }
        var blob = api.uploadComplete(id, f.name, f.size, f.mimeType)
        var waits = 0
        while (blob.sealing && waits++ < 60) { delay(3000); blob = api.uploadComplete(id, f.name, f.size, f.mimeType) }
        if (blob.url == null) throw ApiError(500, "upload", "no url")
        return blob
    }

    private fun registerPush(t: String) {
        scope.launch { runCatching { api.registerPush(t, platformName) } }
    }

    fun signOut() {
        val push = PushBridge.token.value
        val bearer = token
        if (push != null && bearer != null) scope.launch { runCatching { api.unregisterPush(push, bearer) } }
        token = null
        Prefs.set("token", null)
        user = null
        home = null
        tab = Tab.HOME
        replaceAll(Screen.Login)
    }

    /** German is the base language of the whole system; the client can switch any time. */
    private fun defaultLang(): String = "de"
}
