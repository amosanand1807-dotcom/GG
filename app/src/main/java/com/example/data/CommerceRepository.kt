package com.example.data

import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.models.ChatMessage
import com.example.models.CityEvent
import com.example.models.Conversation
import com.example.models.LocalBusiness
import com.example.models.MarketplaceListing
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

class CommerceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in with Google.")

    fun observeCityListings(city: String): Flow<List<MarketplaceListing>> =
        db.collection("listings")
            .whereEqualTo("city", city)
            .whereEqualTo("status", "active")
            .limit(60)
            .snapshots()
            .map { snap ->
                snap.toObjects(MarketplaceListing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeConversations(uid: String): Flow<List<Conversation>> =
        db.collection("conversations")
            .whereArrayContains("participantIds", uid)
            .limit(40)
            .snapshots()
            .map { snap ->
                snap.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeMessages(conversationId: String, uid: String): Flow<List<ChatMessage>> =
        db.collection("conversations").document(conversationId)
            .collection("messages")
            .whereArrayContains("participantIds", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeCityEvents(city: String): Flow<List<CityEvent>> =
        db.collection("events")
            .whereEqualTo("city", city)
            .whereEqualTo("status", "active")
            .limit(40)
            .snapshots()
            .map { snap ->
                snap.toObjects(CityEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeEventRsvp(eventId: String, uid: String): Flow<Boolean> =
        db.collection("events").document(eventId)
            .collection("rsvps").document(uid)
            .snapshots()
            .map { it.exists() }

    suspend fun refreshCityEvents(city: String): Result<List<CityEvent>> {
        val path = "events"
        return try {
            requireUserId()
            val snap = db.collection("events")
                .whereEqualTo("city", city)
                .whereEqualTo("status", "active")
                .limit(40)
                .get(Source.SERVER)
                .await()
            val events = snap.toObjects(CityEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            Result.success(events)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun fetchRsvpedEventIds(eventIds: List<String>): Set<String> {
        val uid = auth.currentUser?.uid ?: return emptySet()
        if (eventIds.isEmpty()) return emptySet()
        return coroutineScope {
            eventIds.map { eventId ->
                async {
                    try {
                        val exists = db.collection("events")
                            .document(eventId)
                            .collection("rsvps")
                            .document(uid)
                            .get()
                            .await()
                            .exists()
                        if (exists) eventId else null
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                }
            }.awaitAll().filterNotNull().toSet()
        }
    }

    fun observeCityBusinesses(city: String): Flow<List<LocalBusiness>> =
        db.collection("businesses")
            .whereEqualTo("city", city)
            .whereEqualTo("status", "active")
            .limit(40)
            .snapshots()
            .map { snap ->
                snap.toObjects(LocalBusiness::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.verified }
            }

    suspend fun createListing(
        profile: UserProfile,
        title: String,
        description: String,
        priceInr: Long,
        category: String,
        condition: String,
        neighborhood: String,
        mediaUri: String?
    ): Result<Unit> {
        val path = "listings"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim()
            val cleanDesc = description.trim()
            require(cleanTitle.length in 2..100 && cleanDesc.isNotEmpty() && cleanDesc.length <= 600)
            require(priceInr in 0L..10_000_000L)

            val payload = mutableMapOf<String, Any?>(
                "sellerId" to uid,
                "sellerName" to profile.displayName.ifBlank { "Seller" }.take(60),
                "title" to cleanTitle,
                "description" to cleanDesc,
                "priceInr" to priceInr,
                "category" to category.take(40),
                "condition" to condition,
                "city" to profile.city.take(50),
                "neighborhood" to neighborhood.trim().take(60).ifBlank { profile.neighborhood.take(60) },
                "mediaUri" to mediaUri?.take(500),
                "status" to "active",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            @Suppress("UNCHECKED_CAST")
            db.collection("listings").document()
                .set(payload.filterValues { it != null } as Map<String, Any>)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun markListingSold(listingId: String): Result<Unit> {
        val path = "listings/$listingId"
        return try {
            requireUserId()
            db.collection("listings").document(listingId).update(
                mapOf(
                    "status" to "sold",
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

    suspend fun startOrGetConversation(
        myProfile: UserProfile,
        otherUserId: String,
        otherUserName: String,
        contextTitle: String
    ): Result<Conversation> {
        val path = "conversations"
        return try {
            val uid = requireUserId()
            require(otherUserId.isNotBlank() && otherUserId != uid) { "Cannot message yourself." }
            val sortedIds = listOf(uid, otherUserId).sorted()
            val convId = "${sortedIds[0]}_${sortedIds[1]}".take(120)
            val docRef = db.collection("conversations").document(convId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (existing != null) return Result.success(existing)
            }
            val orderedNames = if (sortedIds[0] == uid) {
                listOf(myProfile.displayName.ifBlank { "Me" }.take(50), otherUserName.ifBlank { "Neighbor" }.take(50))
            } else {
                listOf(otherUserName.ifBlank { "Neighbor" }.take(50), myProfile.displayName.ifBlank { "Me" }.take(50))
            }
            val payload = mapOf(
                "participantIds" to sortedIds,
                "participantNames" to orderedNames,
                "contextTitle" to contextTitle.take(100),
                "lastMessage" to "Started conversation",
                "lastSenderId" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            val created = docRef.get().await().toObject(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: Conversation(
                    id = convId,
                    participantIds = sortedIds,
                    participantNames = orderedNames,
                    contextTitle = contextTitle,
                    lastMessage = "Started conversation",
                    lastSenderId = uid
                )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        conversation: Conversation,
        myName: String,
        text: String
    ): Result<Unit> {
        val path = "conversations/${conversation.id}/messages"
        return try {
            val uid = requireUserId()
            val clean = text.trim()
            require(clean.isNotEmpty() && clean.length <= 1000)
            val convRef = db.collection("conversations").document(conversation.id)
            val msgRef = convRef.collection("messages").document()
            msgRef.set(
                mapOf(
                    "conversationId" to conversation.id,
                    "participantIds" to conversation.participantIds,
                    "senderId" to uid,
                    "senderName" to myName.ifBlank { "Neighbor" }.take(60),
                    "text" to clean,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            convRef.update(
                mapOf(
                    "lastMessage" to clean.take(500),
                    "lastSenderId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val recipientId = conversation.participantIds.firstOrNull { it != uid }
            if (!recipientId.isNullOrBlank()) {
                db.collection("users").document(recipientId)
                    .collection("notifications").document()
                    .set(
                        mapOf(
                            "userId" to recipientId,
                            "actorId" to uid,
                            "actorName" to myName.take(60).ifBlank { "Neighbor" },
                            "type" to "message",
                            "title" to "New message from $myName",
                            "body" to clean.take(160),
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

    suspend fun createEvent(
        profile: UserProfile,
        title: String,
        description: String,
        venue: String,
        dateLabel: String,
        category: String,
        priceInr: Long
    ): Result<Unit> {
        val path = "events"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim()
            val cleanDesc = description.trim()
            require(cleanTitle.length in 2..100 && cleanDesc.isNotEmpty() && cleanDesc.length <= 600)
            val docRef = db.collection("events").document()
            docRef.set(
                mapOf(
                    "organizerId" to uid,
                    "organizerName" to profile.displayName.ifBlank { "Host" }.take(60),
                    "title" to cleanTitle,
                    "description" to cleanDesc,
                    "city" to profile.city.take(50),
                    "venue" to venue.trim().take(120).ifBlank { profile.city },
                    "dateLabel" to dateLabel.trim().take(60).ifBlank { "This Weekend" },
                    "category" to category.take(40),
                    "priceInr" to priceInr.coerceIn(0L, 500_000L),
                    "rsvpCount" to 1L,
                    "status" to "active",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            docRef.collection("rsvps").document(uid).set(
                mapOf(
                    "userId" to uid,
                    "displayName" to profile.displayName.ifBlank { "Host" }.take(60),
                    "createdAt" to FieldValue.serverTimestamp()
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

    suspend fun toggleEventRsvp(event: CityEvent, profile: UserProfile): Result<Boolean> {
        val path = "events/${event.id}/rsvps"
        return try {
            val uid = requireUserId()
            require(event.id.isNotBlank()) { "Event ID cannot be blank." }
            val rsvpRef = db.collection("events").document(event.id).collection("rsvps").document(uid)
            val eventRef = db.collection("events").document(event.id)
            val joined = db.runTransaction { tx ->
                val eventSnap = tx.get(eventRef)
                val rsvpSnap = tx.get(rsvpRef)
                val currentCount = (eventSnap.getLong("rsvpCount") ?: event.rsvpCount).coerceAtLeast(0L)
                if (rsvpSnap.exists()) {
                    tx.delete(rsvpRef)
                    tx.update(
                        eventRef,
                        mapOf(
                            "rsvpCount" to (currentCount - 1L).coerceAtLeast(0L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    false
                } else {
                    tx.set(
                        rsvpRef,
                        mapOf(
                            "userId" to uid,
                            "displayName" to profile.displayName.ifBlank { "Guest" }.take(60),
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.update(
                        eventRef,
                        mapOf(
                            "rsvpCount" to currentCount + 1L,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    true
                }
            }.await()
            Result.success(joined)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createBusiness(
        profile: UserProfile,
        name: String,
        category: String,
        description: String,
        address: String,
        offerTitle: String,
        offerCode: String,
        discountLabel: String
    ): Result<Unit> {
        val path = "businesses"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim()
            val cleanDesc = description.trim()
            require(cleanName.length in 2..80 && cleanDesc.isNotEmpty() && cleanDesc.length <= 400)
            val payload = mutableMapOf<String, Any?>(
                "ownerId" to uid,
                "name" to cleanName,
                "category" to category.take(40),
                "description" to cleanDesc,
                "city" to profile.city.take(50),
                "address" to address.trim().take(160).ifBlank { profile.city },
                "offerTitle" to offerTitle.trim().take(100),
                "offerCode" to offerCode.trim().uppercase().take(30),
                "discountLabel" to discountLabel.trim().take(40),
                "verified" to false,
                "status" to "active",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            @Suppress("UNCHECKED_CAST")
            db.collection("businesses").document()
                .set(payload.filterValues { it != null } as Map<String, Any>)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminVerifyBusiness(businessId: String, verified: Boolean): Result<Unit> {
        val path = "businesses/$businessId"
        return try {
            requireUserId()
            db.collection("businesses").document(businessId).update(
                mapOf(
                    "verified" to verified,
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
