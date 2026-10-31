package de.focusontop.client.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull

/* The server is SQLite-backed: numbers can come back as 0/1, "12.5" or 12.5.
   Fields whose type varies are JsonElement and read through these helpers. */
fun JsonElement?.asDouble(): Double? = (this as? JsonPrimitive)?.let { p -> p.doubleOrNull ?: p.content.toDoubleOrNull() }
fun JsonElement?.asBool(): Boolean = (this as? JsonPrimitive)?.let { p ->
    p.booleanOrNull ?: (p.content == "1" || p.content.equals("true", ignoreCase = true))
} ?: false

@Serializable
data class User(
    val id: String = "",
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val company: String? = null,
    val avatar: String? = null,
    val role: String? = null,
    val status: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class LoginResponse(
    val token: String? = null,
    val user: User? = null,
    val requires2FA: Boolean? = null,
    val userId: String? = null,
)

@Serializable
data class Task(
    val id: String = "",
    val title: String? = null,
    val status: String? = null,
    val deliveryDate: String? = null,
    val projectName: String? = null,
    val contentType: String? = null,
)

@Serializable
data class ClientFile(
    val id: String = "",
    val name: String? = null,
    val fileName: String? = null,
    val type: String? = null,           // Video | Image | Document | Link
    val url: String? = null,
    val previewUrl: String? = null,
    val hlsUrl: String? = null,
    val thumbnail: String? = null,
    val status: String? = null,         // ready_for_review | revision_requested | approved | delivered | published
    val revisionNotes: String? = null,
    val taskName: String? = null,
    val projectName: String? = null,
    val size: String? = null,
    val byteSize: JsonElement? = null,
    val mimeType: String? = null,
    val category: String? = null,
    val mine: Boolean = false,
    val commentCount: Int = 0,
    val createdAt: String? = null,
    val taskId: String? = null,
    val versionLabel: String? = null,
)

@Serializable
data class Invoice(
    val id: String = "",
    val invoiceNumber: String? = null,
    val total: JsonElement? = null,
    val amount: JsonElement? = null,
    val currency: String? = null,
    val status: String? = null,
    val dueDate: String? = null,
    val issuedDate: String? = null,
    val attachmentUrl: String? = null,
)

@Serializable
data class Subscription(
    val id: String = "",
    val status: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val autoRenew: JsonElement? = null,
    val contractStatus: String? = null,
    val contractToken: String? = null,
    val pausedFrom: String? = null,
    val pausedUntil: String? = null,
    val price: JsonElement? = null,
    val currency: String? = null,
    val billingCycle: String? = null,
    val nameAr: String? = null,
    val nameEn: String? = null,
    val nameDe: String? = null,
)

@Serializable
data class Manager(
    val id: String = "",
    val name: String? = null,
    val avatar: String? = null,
    val job: String? = null,
    val specialization: String? = null,
)

@Serializable
data class Home(
    val user: User? = null,
    val manager: Manager? = null,
    val tasks: List<Task> = emptyList(),
    val pendingReview: List<ClientFile> = emptyList(),
    val files: List<ClientFile> = emptyList(),
    val invoices: List<Invoice> = emptyList(),
    val subscription: Subscription? = null,
    val unreadNotifications: Int = 0,
    val unreadMessages: Int = 0,
    val appointments: List<Appointment> = emptyList(),
    val usage: Usage? = null,
    val stories: List<Story> = emptyList(),
    val stats: Stats? = null,
)

/** What the plan allows and how much of it is used (limits null = no limit set). */
@Serializable
data class Usage(
    val storageUsedBytes: Double = 0.0,
    val storageLimitGb: Double? = null,
    val reelsThisMonth: Int = 0,
    val reelsLimit: Double? = null,
    val shootHoursThisMonth: Double = 0.0,
    val shootHoursLimit: Double? = null,
    val monthIndex: Int? = null,
    val monthsTotal: Int? = null,
    val daysLeft: Int? = null,
)

/** A circle in the stories row: the newest work of one project / folder. */
@Serializable
data class Story(
    val id: String = "",
    val title: String = "",
    val cover: String? = null,
    val count: Int = 0,
    val fresh: Boolean = false,
    val fileIds: List<String> = emptyList(),
)

@Serializable
data class Stats(val files: Int = 0, val approved: Int = 0, val projects: Int = 0, val uploads: Int = 0)

@Serializable
data class ChatMessage(
    val id: String = "",
    val senderId: String? = null,
    val senderName: String? = null,
    val type: String? = null,
    val text: String? = null,
    val imageUrl: String? = null,
    val fileName: String? = null,
    val fileData: String? = null,
    val createdAt: String? = null,
    val mine: Boolean = false,
)

@Serializable
data class ChatThread(val chatId: String = "", val messages: List<ChatMessage> = emptyList())

@Serializable
data class Slot(val time: String = "", val free: Boolean = false)

@Serializable
data class Availability(val date: String = "", val slots: List<Slot> = emptyList())

/** Result of /api/upload/complete. */
@Serializable
data class UploadedBlob(
    val url: String? = null,
    val filename: String? = null,
    val byteSize: Long? = null,
    val mimeType: String? = null,
    val checksum: String? = null,
    val sealing: Boolean = false,
)

@Serializable
data class UploadInit(val uploadId: String = "", val received: Long = 0)

/** One upcoming appointment; date/times are already Berlin local time from the server. */
@Serializable
data class Appointment(
    val id: String = "",
    val title: String? = null,
    val description: String? = null,
    val type: String? = null,
    val date: String = "",          // YYYY-MM-DD
    val time: String? = null,       // HH:mm (null = all day)
    val endTime: String? = null,
    val allDay: Boolean = false,
    val location: String? = null,
    val videoLink: String? = null,
    val status: String? = null,     // confirmed | requested
)

@Serializable
data class AppNotification(
    val id: String = "",
    val title: String? = null,
    val message: String? = null,
    val type: String? = null,
    val isRead: JsonElement? = null,
    val relatedId: String? = null,
    val relatedType: String? = null,
    val createdAt: String? = null,
    val kind: String? = null,          // files | appointments | invoices | messages | other
    val senderName: String? = null,
    val thumbnail: String? = null,
    val fileStatus: String? = null,
)

@Serializable
data class Comment(
    val id: String = "",
    val userName: String? = null,
    val userRole: String? = null,
    val text: String? = null,
    val createdAt: String? = null,
    val timestampSeconds: Double? = null,
    val resolved: Int = 0,
)

@Serializable
data class ClientConfig(
    val googleWebClientId: String? = null,
    val googleIosClientId: String? = null,
    val firebaseApiKey: String? = null,
    val googleSignIn: Boolean = false,
)

@Serializable
data class FirebaseIdpResponse(val idToken: String? = null)

/* ── Community (stories, posts, follows) ── */

@Serializable
data class CAuthor(val id: String? = null, val name: String = "", val avatar: String? = null, val team: Boolean = false)

@Serializable
data class CTag(val id: String = "", val name: String = "")

@Serializable
data class CPost(
    val id: String = "",
    val kind: String = "post",          // post | story
    val author: CAuthor = CAuthor(),
    val mine: Boolean = false,
    val mediaUrl: String? = null,
    val mediaType: String? = null,      // Image | Video
    val caption: String = "",
    val visibility: String = "account", // account | public
    val likes: Int = 0,
    val liked: Boolean = false,
    val comments: Int = 0,
    val saved: Boolean = false,
    val tags: List<CTag> = emptyList(),
    val createdAt: String? = null,
    val expiresAt: String? = null,
    val seen: Boolean = false,
)

@Serializable
data class CStoryGroup(val author: CAuthor = CAuthor(), val items: List<CPost> = emptyList(), val allSeen: Boolean = false)

@Serializable
data class CComment(val id: String = "", val text: String = "", val createdAt: String? = null, val mine: Boolean = false, val author: CAuthor = CAuthor())

@Serializable
data class CProfile(
    val id: String = "",
    val name: String = "",
    val avatar: String? = null,
    val private: Boolean = false,
    val me: Boolean = false,
    val since: String? = null,
    val followers: Int = 0,
    val following: Int = 0,
    val posts: List<CPost> = emptyList(),
    val isFollowing: Boolean = false,
)

@Serializable
data class CMe(val private: Boolean = true, val followers: Int = 0, val following: Int = 0, val posts: List<CPost> = emptyList())

@Serializable
data class CPerson(val id: String = "", val name: String = "", val avatar: String? = null, val following: Boolean = false)

@Serializable
data class FollowState(val following: Boolean = false)

/** A viewer row in the Reel/post "Insights" sheet (Figma "F2 · Medien · Statistik und Zuschauer"). */
@Serializable
data class CViewer(val id: String = "", val name: String = "", val avatar: String? = null, val following: Boolean = false, val liked: Boolean = false)

@Serializable
data class CInsights(val views: Int = 0, val likes: Int = 0, val comments: Int = 0, val saves: Int = 0, val shares: Int = 0, val viewers: List<CViewer> = emptyList())
