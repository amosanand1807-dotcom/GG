package com.example.data

import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.models.AppNotification
import com.example.models.FollowEdge
import com.example.models.SavedItem
import com.example.models.UserPrivateSettings
import com.example.models.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in with Google.")

    fun observeProfile(uid: String): Flow<UserProfile?> =
        db.collection("users").document(uid)
            .snapshots()
            .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

    fun observeCityNeighbors(city: String): Flow<List<UserProfile>> =
        db.collection("users")
            .whereEqualTo("city", city)
            .whereEqualTo("onboardingDone", true)
            .limit(40)
            .snapshots()
            .map { snap ->
                snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.followersCount }
            }

    fun observePrivateSettings(uid: String): Flow<UserPrivateSettings?> =
        db.collection("users").document(uid)
            .collection("private").document("account")
            .snapshots()
            .map { it.toObject(UserPrivateSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

    fun observeFollowing(uid: String): Flow<List<FollowEdge>> =
        db.collection("users").document(uid)
            .collection("following")
            .snapshots()
            .map { snap ->
                snap.toObjects(FollowEdge::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeSavedItems(uid: String): Flow<List<SavedItem>> =
        db.collection("users").document(uid)
            .collection("saved")
            .snapshots()
            .map { snap ->
                snap.toObjects(SavedItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeNotifications(uid: String): Flow<List<AppNotification>> =
        db.collection("users").document(uid)
            .collection("notifications")
            .snapshots()
            .map { snap ->
                snap.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    suspend fun saveOnboardingProfile(
        displayName: String,
        handle: String,
        city: String,
        neighborhood: String,
        bio: String,
        avatarEmoji: String,
        interests: List<String>,
        languageCode: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}"
        return try {
            val resolvedUid = requireUserId()
            val cleanName = displayName.trim().ifBlank { "CityLoop Neighbor" }.take(50)
            val cleanHandle = handle.trim().lowercase().replace(Regex("[^a-z0-9_]"), "").ifBlank {
                "neighbor_${resolvedUid.take(5)}"
            }.take(30)
            val docRef = db.collection("users").document(resolvedUid)
            val existing = docRef.get().await()

            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "displayName" to cleanName,
                        "handle" to cleanHandle,
                        "bio" to bio.trim().take(240),
                        "city" to city,
                        "neighborhood" to neighborhood.trim().take(60),
                        "avatarEmoji" to avatarEmoji.take(12),
                        "interests" to interests.take(8),
                        "onboardingDone" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val profile = UserProfile(
                    userId = resolvedUid,
                    displayName = cleanName,
                    handle = cleanHandle,
                    bio = bio.trim().take(240),
                    city = city,
                    neighborhood = neighborhood.trim().take(60),
                    avatarEmoji = avatarEmoji.take(12),
                    interests = interests.take(8),
                    role = "member",
                    planTier = "free",
                    onboardingDone = true
                )
                docRef.set(profile.toCreateMap()).await()
            }

            // Ensure owner-isolated private account doc exists
            val privateRef = docRef.collection("private").document("account")
            val privateSnap = privateRef.get().await()
            val email = auth.currentUser?.email.orEmpty().take(120)
            if (!privateSnap.exists()) {
                privateRef.set(
                    mapOf(
                        "userId" to resolvedUid,
                        "email" to email,
                        "languageCode" to if (languageCode == "hi") "hi" else "en",
                        "darkThemeMode" to "system",
                        "pushEnabled" to true,
                        "privateProfile" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Welcome notification if first onboarding
            val welcomeRef = docRef.collection("notifications").document("welcome_${resolvedUid.take(8)}")
            if (!welcomeRef.get().await().exists()) {
                welcomeRef.set(
                    mapOf(
                        "userId" to resolvedUid,
                        "actorId" to resolvedUid,
                        "actorName" to "CITYLOOP",
                        "type" to "system",
                        "title" to "Welcome to $city on CITYLOOP",
                        "body" to "Explore local posts, communities, bazaar deals, and neighborhood events.",
                        "readFlag" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        displayName: String,
        bio: String,
        city: String,
        neighborhood: String,
        avatarEmoji: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}"
        return try {
            val resolvedUid = requireUserId()
            db.collection("users").document(resolvedUid).update(
                mapOf(
                    "displayName" to displayName.trim().take(50).ifBlank { "Neighbor" },
                    "bio" to bio.trim().take(240),
                    "city" to city,
                    "neighborhood" to neighborhood.trim().take(60),
                    "avatarEmoji" to avatarEmoji.take(12),
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

    suspend fun updatePrivateSettings(
        languageCode: String? = null,
        darkThemeMode: String? = null,
        pushEnabled: Boolean? = null,
        privateProfile: Boolean? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}/private/account"
        return try {
            val resolvedUid = requireUserId()
            val ref = db.collection("users").document(resolvedUid).collection("private").document("account")
            val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            if (languageCode != null) updates["languageCode"] = languageCode
            if (darkThemeMode != null) updates["darkThemeMode"] = darkThemeMode
            if (pushEnabled != null) updates["pushEnabled"] = pushEnabled
            if (privateProfile != null) updates["privateProfile"] = privateProfile
            if (ref.get().await().exists()) {
                ref.update(updates).await()
            } else {
                ref.set(
                    mapOf(
                        "userId" to resolvedUid,
                        "email" to (auth.currentUser?.email.orEmpty()),
                        "languageCode" to (languageCode ?: "en"),
                        "darkThemeMode" to (darkThemeMode ?: "system"),
                        "pushEnabled" to (pushEnabled ?: true),
                        "privateProfile" to (privateProfile ?: false),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleFollow(target: UserProfile, currentlyFollowing: Boolean, myName: String): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}/following/${target.userId}"
        return try {
            val resolvedUid = requireUserId()
            require(target.userId.isNotBlank() && target.userId != resolvedUid) { "Invalid target user." }
            val followRef = db.collection("users").document(resolvedUid).collection("following").document(target.userId)
            val myRef = db.collection("users").document(resolvedUid)
            val targetRef = db.collection("users").document(target.userId)

            if (currentlyFollowing) {
                followRef.delete().await()
                myRef.update(
                    mapOf(
                        "followingCount" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                targetRef.update(
                    mapOf(
                        "followersCount" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                followRef.set(
                    mapOf(
                        "userId" to resolvedUid,
                        "targetUserId" to target.userId,
                        "targetName" to target.displayName.take(60),
                        "targetHandle" to target.handle.take(40),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                myRef.update(
                    mapOf(
                        "followingCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                targetRef.update(
                    mapOf(
                        "followersCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                // Notify target user
                val notifRef = targetRef.collection("notifications").document()
                notifRef.set(
                    mapOf(
                        "userId" to target.userId,
                        "actorId" to resolvedUid,
                        "actorName" to myName.take(60).ifBlank { "Someone" },
                        "type" to "follow",
                        "title" to "New follower",
                        "body" to "$myName started following you on CITYLOOP.",
                        "readFlag" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleSaveItem(
        itemType: String,
        targetId: String,
        title: String,
        subtitle: String,
        city: String,
        isCurrentlySaved: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        val docId = "${itemType}_${targetId}"
        val path = "users/${uid ?: "unauthenticated"}/saved/$docId"
        return try {
            val resolvedUid = requireUserId()
            val ref = db.collection("users").document(resolvedUid).collection("saved").document(docId)
            if (isCurrentlySaved) {
                ref.delete().await()
            } else {
                ref.set(
                    mapOf(
                        "userId" to resolvedUid,
                        "itemType" to itemType,
                        "targetId" to targetId.take(128),
                        "title" to title.take(140).ifBlank { "Saved item" },
                        "subtitle" to subtitle.take(200),
                        "city" to city.take(50),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notifId: String): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}/notifications/$notifId"
        return try {
            val resolvedUid = requireUserId()
            db.collection("users").document(resolvedUid)
                .collection("notifications").document(notifId)
                .update("readFlag", true)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleBlockUser(targetUserId: String, currentBlocked: List<String>): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}"
        return try {
            val resolvedUid = requireUserId()
            val updated = if (targetUserId in currentBlocked) {
                currentBlocked.filterNot { it == targetUserId }
            } else {
                (currentBlocked + targetUserId).distinct().take(50)
            }
            db.collection("users").document(resolvedUid)
                .update(
                    mapOf(
                        "blockedUserIds" to updated,
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

    suspend fun updatePlanTier(planTier: String): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}"
        return try {
            val resolvedUid = requireUserId()
            require(planTier in listOf("free", "plus"))
            db.collection("users").document(resolvedUid)
                .update(
                    mapOf(
                        "planTier" to planTier,
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

    suspend fun deleteAccountData(): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}"
        return try {
            val resolvedUid = requireUserId()
            val userDoc = db.collection("users").document(resolvedUid)
            userDoc.collection("private").document("account").delete().await()
            userDoc.delete().await()
            auth.currentUser?.delete()?.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
