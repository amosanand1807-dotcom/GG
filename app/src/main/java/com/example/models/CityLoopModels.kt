package com.example.models

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val handle: String = "",
    val bio: String = "",
    val city: String = "Bengaluru",
    val neighborhood: String = "",
    val avatarEmoji: String = "🧑🏽‍💻",
    val interests: List<String> = emptyList(),
    val role: String = "member",
    val planTier: String = "free",
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val postsCount: Long = 0L,
    val blockedUserIds: List<String> = emptyList(),
    val onboardingDone: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName.take(50),
        "handle" to handle.take(30),
        "bio" to bio.take(240),
        "city" to city.take(50),
        "neighborhood" to neighborhood.take(60),
        "avatarEmoji" to avatarEmoji.take(12),
        "interests" to interests.take(8),
        "role" to role,
        "planTier" to planTier,
        "followersCount" to followersCount.coerceAtLeast(0L),
        "followingCount" to followingCount.coerceAtLeast(0L),
        "postsCount" to postsCount.coerceAtLeast(0L),
        "blockedUserIds" to blockedUserIds.take(50),
        "onboardingDone" to onboardingDone,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class UserPrivateSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val phone: String = "",
    val languageCode: String = "en",
    val darkThemeMode: String = "system",
    val pushEnabled: Boolean = true,
    val privateProfile: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FollowEdge(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetUserId: String = "",
    val targetName: String = "",
    val targetHandle: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val itemType: String = "post",
    val targetId: String = "",
    val title: String = "",
    val subtitle: String = "",
    val city: String = "Bengaluru",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val type: String = "system",
    val title: String = "",
    val body: String = "",
    val readFlag: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CityPost(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorHandle: String = "",
    val authorEmoji: String = "🧑🏽‍💻",
    val city: String = "Bengaluru",
    val neighborhood: String = "",
    val postType: String = "text", // text, photo, reel
    val caption: String = "",
    val mediaUri: String? = null,
    val videoDurationSec: Long = 0L,
    val tags: List<String> = emptyList(),
    val communityId: String = "",
    val communityName: String = "",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val status: String = "active", // active, flagged, removed
    val moderationNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorEmoji: String = "🧑🏽‍💻",
    val body: String = "",
    val parentCommentId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CityCommunity(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val name: String = "",
    val description: String = "",
    val city: String = "Bengaluru",
    val category: String = "Neighborhood",
    val emoji: String = "🏘️",
    val memberCount: Long = 1L,
    val status: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MarketplaceListing(
    @DocumentId val id: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val title: String = "",
    val description: String = "",
    val priceInr: Long = 0L,
    val category: String = "Furniture",
    val condition: String = "Like New",
    val city: String = "Bengaluru",
    val neighborhood: String = "",
    val mediaUri: String? = null,
    val status: String = "active", // active, sold, removed
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: List<String> = emptyList(),
    val contextTitle: String = "",
    val lastMessage: String = "",
    val lastSenderId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val participantIds: List<String> = emptyList(),
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CityEvent(
    @DocumentId val id: String = "",
    val organizerId: String = "",
    val organizerName: String = "",
    val title: String = "",
    val description: String = "",
    val city: String = "Bengaluru",
    val venue: String = "",
    val dateLabel: String = "",
    val category: String = "Meetup",
    val priceInr: Long = 0L,
    val rsvpCount: Long = 0L,
    val status: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LocalBusiness(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val category: String = "Cafe & Chai",
    val description: String = "",
    val city: String = "Bengaluru",
    val address: String = "",
    val offerTitle: String = "",
    val offerCode: String = "",
    val discountLabel: String = "",
    val verified: Boolean = false,
    val status: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ModerationReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetType: String = "post",
    val targetId: String = "",
    val reason: String = "",
    val city: String = "Bengaluru",
    val status: String = "open",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
