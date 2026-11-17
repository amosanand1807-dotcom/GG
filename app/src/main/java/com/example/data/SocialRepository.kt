package com.example.data

import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.core.matchesPostSearchQuery
import com.example.models.CityCommunity
import com.example.models.CityPost
import com.example.models.ModerationReport
import com.example.models.PostComment
import com.example.models.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SocialRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in with Google.")

    fun observeCityPosts(city: String): Flow<List<CityPost>> =
        db.collection("posts")
            .whereEqualTo("city", city)
            .whereEqualTo("status", "active")
            .limit(60)
            .snapshots()
            .map { snap ->
                snap.toObjects(CityPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun searchCityPosts(city: String, query: String): Flow<List<CityPost>> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return observeCityPosts(city)
        return db.collection("posts")
            .whereEqualTo("city", city)
            .whereEqualTo("status", "active")
            .limit(150)
            .snapshots()
            .map { snap ->
                snap.toObjects(CityPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { matchesPostSearchQuery(it, trimmed) }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
    }

    suspend fun refreshCityPosts(city: String, query: String = ""): Result<List<CityPost>> {
        val path = "posts"
        return try {
            requireUserId()
            val trimmed = query.trim()
            val fetchLimit = if (trimmed.isBlank()) 60L else 150L
            val snap = db.collection("posts")
                .whereEqualTo("city", city)
                .whereEqualTo("status", "active")
                .limit(fetchLimit)
                .get(Source.SERVER)
                .await()
            val posts = snap.toObjects(CityPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .let { list ->
                    if (trimmed.isBlank()) list else list.filter { matchesPostSearchQuery(it, trimmed) }
                }
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            Result.success(posts)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    fun observeUserPosts(uid: String): Flow<List<CityPost>> =
        db.collection("posts")
            .whereEqualTo("authorId", uid)
            .limit(40)
            .snapshots()
            .map { snap ->
                snap.toObjects(CityPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observePostComments(postId: String): Flow<List<PostComment>> =
        db.collection("posts").document(postId)
            .collection("comments")
            .snapshots()
            .map { snap ->
                snap.toObjects(PostComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeUserLikeForPost(postId: String, uid: String): Flow<Boolean> =
        db.collection("posts").document(postId)
            .collection("likes").document(uid)
            .snapshots()
            .map { it.exists() }

    suspend fun fetchLikedPostIds(postIds: List<String>): Set<String> {
        val uid = auth.currentUser?.uid ?: return emptySet()
        if (postIds.isEmpty()) return emptySet()
        return coroutineScope {
            postIds.map { postId ->
                async {
                    try {
                        val exists = db.collection("posts")
                            .document(postId)
                            .collection("likes")
                            .document(uid)
                            .get()
                            .await()
                            .exists()
                        if (exists) postId else null
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                }
            }.awaitAll().filterNotNull().toSet()
        }
    }

    fun observeCityCommunities(city: String): Flow<List<CityCommunity>> =
        db.collection("communities")
            .whereEqualTo("city", city)
            .whereEqualTo("status", "active")
            .limit(40)
            .snapshots()
            .map { snap ->
                snap.toObjects(CityCommunity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.memberCount }
            }

    fun observeCommunityMembership(communityId: String, uid: String): Flow<Boolean> =
        db.collection("communities").document(communityId)
            .collection("members").document(uid)
            .snapshots()
            .map { it.exists() }

    fun observeReports(isAdmin: Boolean, uid: String): Flow<List<ModerationReport>> {
        val query = if (isAdmin) {
            db.collection("reports").limit(60)
        } else {
            db.collection("reports").whereEqualTo("reporterId", uid).limit(40)
        }
        return query.snapshots().map { snap ->
            snap.toObjects(ModerationReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
    }

    suspend fun createPost(
        profile: UserProfile,
        postType: String,
        caption: String,
        neighborhood: String,
        mediaUri: String?,
        videoDurationSec: Long,
        tags: List<String>,
        communityId: String,
        communityName: String
    ): Result<String> {
        val path = "posts"
        return try {
            val uid = requireUserId()
            val cleanCaption = caption.trim()
            require(cleanCaption.isNotEmpty() && cleanCaption.length <= 1000) {
                "Caption must be between 1 and 1000 characters."
            }
            val docRef = db.collection("posts").document()
            val payload = mutableMapOf<String, Any?>(
                "authorId" to uid,
                "authorName" to profile.displayName.ifBlank { "Neighbor" }.take(60),
                "authorHandle" to profile.handle.ifBlank { "neighbor" }.take(40),
                "authorEmoji" to profile.avatarEmoji.take(12),
                "city" to profile.city.take(50),
                "neighborhood" to neighborhood.trim().take(60).ifBlank { profile.neighborhood.take(60) },
                "postType" to postType,
                "caption" to cleanCaption,
                "mediaUri" to mediaUri?.take(500),
                "videoDurationSec" to if (postType == "reel") videoDurationSec.coerceIn(1L, 180L) else 0L,
                "tags" to tags.map { it.trim().take(24) }.filter { it.isNotEmpty() }.take(6),
                "communityId" to communityId.take(128),
                "communityName" to communityName.take(80),
                "likesCount" to 0L,
                "commentsCount" to 0L,
                "status" to "active",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            @Suppress("UNCHECKED_CAST")
            val sanitized = payload.filterValues { it != null } as Map<String, Any>
            docRef.set(sanitized).await()

            // Increment user postsCount
            db.collection("users").document(uid).update(
                mapOf(
                    "postsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleLike(post: CityPost, myName: String): Result<Boolean> {
        val path = "posts/${post.id}/likes"
        return try {
            val uid = requireUserId()
            val likeRef = db.collection("posts").document(post.id).collection("likes").document(uid)
            val postRef = db.collection("posts").document(post.id)
            val alreadyLiked = likeRef.get().await().exists()
            if (alreadyLiked) {
                likeRef.delete().await()
                postRef.update(
                    mapOf(
                        "likesCount" to (post.likesCount - 1L).coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(false)
            } else {
                likeRef.set(
                    mapOf(
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                postRef.update(
                    mapOf(
                        "likesCount" to post.likesCount + 1L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                if (post.authorId != uid && post.authorId.isNotBlank()) {
                    db.collection("users").document(post.authorId)
                        .collection("notifications").document()
                        .set(
                            mapOf(
                                "userId" to post.authorId,
                                "actorId" to uid,
                                "actorName" to myName.take(60).ifBlank { "Someone" },
                                "type" to "like",
                                "title" to "Post liked",
                                "body" to "$myName liked your post in ${post.city}.",
                                "readFlag" to false,
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                }
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun addComment(
        post: CityPost,
        profile: UserProfile,
        body: String,
        parentCommentId: String = ""
    ): Result<Unit> {
        val path = "posts/${post.id}/comments"
        return try {
            val uid = requireUserId()
            val clean = body.trim()
            require(clean.isNotEmpty() && clean.length <= 500) {
                "Comment must be 1..500 characters."
            }
            val commentRef = db.collection("posts").document(post.id).collection("comments").document()
            commentRef.set(
                mapOf(
                    "postId" to post.id,
                    "authorId" to uid,
                    "authorName" to profile.displayName.ifBlank { "Neighbor" }.take(60),
                    "authorEmoji" to profile.avatarEmoji.take(12),
                    "body" to clean,
                    "parentCommentId" to parentCommentId.take(128),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("posts").document(post.id).update(
                mapOf(
                    "commentsCount" to post.commentsCount + 1L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (post.authorId != uid && post.authorId.isNotBlank()) {
                db.collection("users").document(post.authorId)
                    .collection("notifications").document()
                    .set(
                        mapOf(
                            "userId" to post.authorId,
                            "actorId" to uid,
                            "actorName" to profile.displayName.take(60).ifBlank { "Someone" },
                            "type" to "comment",
                            "title" to "New reply",
                            "body" to "${profile.displayName}: ${clean.take(120)}",
                            "readFlag" to false,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        val path = "posts/$postId"
        return try {
            requireUserId()
            db.collection("posts").document(postId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun createCommunity(
        profile: UserProfile,
        name: String,
        description: String,
        category: String,
        emoji: String
    ): Result<Unit> {
        val path = "communities"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim()
            val cleanDesc = description.trim()
            require(cleanName.length in 2..80 && cleanDesc.isNotEmpty() && cleanDesc.length <= 400)
            val docRef = db.collection("communities").document()
            docRef.set(
                mapOf(
                    "creatorId" to uid,
                    "name" to cleanName,
                    "description" to cleanDesc,
                    "city" to profile.city.take(50),
                    "category" to category.take(40),
                    "emoji" to emoji.take(12),
                    "memberCount" to 1L,
                    "status" to "active",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            docRef.collection("members").document(uid).set(
                mapOf(
                    "userId" to uid,
                    "displayName" to profile.displayName.ifBlank { "Creator" }.take(60),
                    "role" to "moderator",
                    "joinedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleJoinCommunity(community: CityCommunity, profile: UserProfile): Result<Boolean> {
        val path = "communities/${community.id}/members"
        return try {
            val uid = requireUserId()
            val memberRef = db.collection("communities").document(community.id).collection("members").document(uid)
            val commRef = db.collection("communities").document(community.id)
            val isMember = memberRef.get().await().exists()
            if (isMember) {
                memberRef.delete().await()
                commRef.update(
                    mapOf(
                        "memberCount" to (community.memberCount - 1L).coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(false)
            } else {
                memberRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to profile.displayName.ifBlank { "Member" }.take(60),
                        "role" to "member",
                        "joinedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                commRef.update(
                    mapOf(
                        "memberCount" to community.memberCount + 1L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun submitReport(
        targetType: String,
        targetId: String,
        reason: String,
        city: String
    ): Result<Unit> {
        val path = "reports"
        return try {
            val uid = requireUserId()
            val cleanReason = reason.trim()
            require(cleanReason.length in 3..300)
            db.collection("reports").document().set(
                mapOf(
                    "reporterId" to uid,
                    "targetType" to targetType,
                    "targetId" to targetId.take(128),
                    "reason" to cleanReason,
                    "city" to city.take(50),
                    "status" to "open",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminModeratePost(postId: String, newStatus: String, note: String): Result<Unit> {
        val path = "posts/$postId"
        return try {
            requireUserId()
            db.collection("posts").document(postId).update(
                mapOf(
                    "status" to newStatus,
                    "moderationNote" to note.take(200),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminResolveReport(reportId: String, newStatus: String): Result<Unit> {
        val path = "reports/$reportId"
        return try {
            requireUserId()
            db.collection("reports").document(reportId).update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
