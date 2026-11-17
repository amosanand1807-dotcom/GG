const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_101";
const BOB_UID = "bob_uid_202";
const ADMIN_UID = "admin_uid_999";
const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 5000) {
  return new Date(Date.now() - msOffset);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("posts").where("city", "==", "Bengaluru").where("status", "==", "active").get());
  await assertFails(unauth.collection("listings").where("city", "==", "Bengaluru").where("status", "==", "active").get());
  await assertFails(unauth.collection("conversations").where("participantIds", "array-contains", ALICE_UID).get());
});

test("User profile creation, PII isolation, and role escalation guard", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const now = pastDate();

  // Valid member profile create (matches UserProfileRepository)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Aarav Mehta",
      handle: "aarav_blr",
      bio: "Coffee & cycling around Indiranagar",
      city: "Bengaluru",
      neighborhood: "Indiranagar",
      avatarEmoji: "🧑🏽‍💻",
      interests: ["Tech", "Cycling"],
      role: "member",
      planTier: "free",
      followersCount: 0,
      followingCount: 0,
      postsCount: 0,
      blockedUserIds: [],
      onboardingDone: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject self-assigned admin role on create
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      handle: "bob_admin",
      city: "Mumbai",
      role: "admin",
      planTier: "free",
      followersCount: 0,
      followingCount: 0,
      postsCount: 0,
      onboardingDone: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject shadow field injection
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      handle: "bob_shadow",
      city: "Mumbai",
      role: "member",
      planTier: "free",
      followersCount: 0,
      followingCount: 0,
      postsCount: 0,
      onboardingDone: true,
      isSuperVerified: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Private PII settings strictly isolated to owner
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("account").set({
      userId: ALICE_UID,
      email: "alice@example.com",
      languageCode: "en",
      darkThemeMode: "system",
      pushEnabled: true,
      privateProfile: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("private").doc("account").get()
  );

  // City user directory query aligned with rules
  await assertSucceeds(
    bobDb.collection("users").where("city", "==", "Bengaluru").where("onboardingDone", "==", true).get()
  );
});

test("Social posts, comments, likes, and moderation boundaries", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "amosanand18@gmail.com",
    email_verified: true,
  }).firestore();
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: "amosanand18@gmail.com",
    email_verified: false,
  }).firestore();
  const now = pastDate();

  // Create active post (without @DocumentId `id` field)
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "Aarav Mehta",
      authorHandle: "aarav_blr",
      authorEmoji: "🧑🏽‍💻",
      city: "Bengaluru",
      neighborhood: "Indiranagar",
      postType: "text",
      caption: "Morning filter coffee meetup at 100ft Road!",
      tags: ["coffee", "indiranagar"],
      likesCount: 0,
      commentsCount: 0,
      status: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject @DocumentId `id` inside payload
  await assertFails(
    aliceDb.collection("posts").doc("post_bad").set({
      id: "post_bad",
      authorId: ALICE_UID,
      authorName: "Aarav",
      authorHandle: "aarav",
      city: "Bengaluru",
      postType: "text",
      caption: "Invalid id field",
      likesCount: 0,
      commentsCount: 0,
      status: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Aligned city feed query and author's own posts query succeed
  await assertSucceeds(
    bobDb.collection("posts").where("city", "==", "Bengaluru").where("status", "==", "active").get()
  );
  await assertSucceeds(
    aliceDb.collection("posts").where("authorId", "==", ALICE_UID).get()
  );

  // Bob can like & comment + update counter
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("likes").doc(BOB_UID).set({
      userId: BOB_UID,
      createdAt: pastDate(2000),
    })
  );
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("comments").doc("c_1").set({
      postId: "post_1",
      authorId: BOB_UID,
      authorName: "Bob",
      body: "Count me in!",
      createdAt: pastDate(1500),
    })
  );
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").update({
      likesCount: 1,
      commentsCount: 1,
      updatedAt: pastDate(1000),
    })
  );

  // Bob cannot edit Alice's caption or delete Alice's post
  await assertFails(
    bobDb.collection("posts").doc("post_1").update({
      caption: "Hacked caption",
      updatedAt: pastDate(500),
    })
  );
  await assertFails(bobDb.collection("posts").doc("post_1").delete());

  // Spoofed unverified admin email cannot moderate
  await assertFails(
    spoofedAdminDb.collection("posts").doc("post_1").update({
      status: "removed",
      moderationNote: "Spoofed",
      updatedAt: pastDate(400),
    })
  );

  // Verified admin can moderate post
  await assertSucceeds(
    adminDb.collection("posts").doc("post_1").update({
      status: "removed",
      moderationNote: "Removed by admin",
      updatedAt: pastDate(300),
    })
  );
});

test("Private 1-to-1 messaging strict participant isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext("eve_uid_303").firestore();
  const now = pastDate();

  await assertSucceeds(
    aliceDb.collection("conversations").doc("conv_ab").set({
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: ["Aarav", "Bob"],
      contextTitle: "Marketplace: Vintage Bicycle",
      lastMessage: "Is this still available?",
      lastSenderId: ALICE_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("conversations").doc("conv_ab").collection("messages").doc("m_1").set({
      conversationId: "conv_ab",
      participantIds: [ALICE_UID, BOB_UID],
      senderId: ALICE_UID,
      senderName: "Aarav",
      text: "Is this still available?",
      createdAt: now,
    })
  );

  // Participants can query their conversations & messages
  await assertSucceeds(
    bobDb.collection("conversations").where("participantIds", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("conversations").doc("conv_ab").collection("messages")
      .where("participantIds", "array-contains", BOB_UID).get()
  );

  // Eve (non-participant) cannot read conversation, read m_1, list unfiltered messages, or write messages
  await assertFails(eveDb.collection("conversations").doc("conv_ab").get());
  await assertFails(eveDb.collection("conversations").doc("conv_ab").collection("messages").doc("m_1").get());
  await assertFails(eveDb.collection("conversations").doc("conv_ab").collection("messages").get());
  await assertFails(
    eveDb.collection("conversations").doc("conv_ab").collection("messages").doc("m_hack").set({
      conversationId: "conv_ab",
      participantIds: [ALICE_UID, BOB_UID],
      senderId: "eve_uid_303",
      senderName: "Eve",
      text: "Snooping",
      createdAt: now,
    })
  );
});

test("Marketplace, Communities, Events, Businesses, and Reports", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "amosanand18@gmail.com",
    email_verified: true,
  }).firestore();
  const now = pastDate();

  // Marketplace listing
  await assertSucceeds(
    aliceDb.collection("listings").doc("list_1").set({
      sellerId: ALICE_UID,
      sellerName: "Aarav",
      title: "Hero Sprint Cycle",
      description: "Well maintained hybrid bicycle in Indiranagar",
      priceInr: 6500,
      category: "Cycles & Mobility",
      condition: "Like New",
      city: "Bengaluru",
      neighborhood: "Indiranagar",
      status: "active",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    bobDb.collection("listings").where("city", "==", "Bengaluru").where("status", "==", "active").get()
  );

  // Local business: cannot self-verify on create
  await assertFails(
    aliceDb.collection("businesses").doc("biz_bad").set({
      ownerId: ALICE_UID,
      name: "Fake Verified Cafe",
      category: "Cafe & Chai",
      description: "Best filter coffee",
      city: "Bengaluru",
      address: "12th Main, Indiranagar",
      verified: true,
      status: "active",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("businesses").doc("biz_1").set({
      ownerId: ALICE_UID,
      name: "Filter Kaapi Corner",
      category: "Cafe & Chai",
      description: "Authentic South Indian filter coffee & snacks",
      city: "Bengaluru",
      address: "12th Main, Indiranagar",
      offerTitle: "20% off Morning Filter Kaapi",
      offerCode: "LOOP20",
      discountLabel: "20% OFF",
      verified: false,
      status: "active",
      createdAt: now,
      updatedAt: now,
    })
  );
  // Owner cannot flip verified to true; admin can
  await assertFails(
    aliceDb.collection("businesses").doc("biz_1").update({
      verified: true,
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    adminDb.collection("businesses").doc("biz_1").update({
      verified: true,
      updatedAt: pastDate(1000),
    })
  );

  // Community & Event
  await assertSucceeds(
    aliceDb.collection("communities").doc("comm_1").set({
      creatorId: ALICE_UID,
      name: "Indiranagar Runners",
      description: "Weekend 5K runs at Cubbon Park",
      city: "Bengaluru",
      category: "Fitness & Outdoors",
      emoji: "🏃🏽",
      memberCount: 1,
      status: "active",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("events").doc("evt_1").set({
      organizerId: ALICE_UID,
      organizerName: "Aarav",
      title: "Cubbon Park Sunday Book Swap",
      description: "Bring 2 books, meet readers in Bengaluru.",
      city: "Bengaluru",
      venue: "Cubbon Park Bandstand",
      dateLabel: "Sun, 8:30 AM",
      category: "Books & Culture",
      priceInr: 0,
      rsvpCount: 1,
      status: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reports: Bob can file report; Bob can read own; Alice cannot read Bob's report; Admin can resolve
  await assertSucceeds(
    bobDb.collection("reports").doc("rep_1").set({
      reporterId: BOB_UID,
      targetType: "post",
      targetId: "post_1",
      reason: "Spam link in description",
      city: "Bengaluru",
      status: "open",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    aliceDb.collection("reports").where("reporterId", "==", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("reports").where("reporterId", "==", BOB_UID).get()
  );
  await assertSucceeds(
    adminDb.collection("reports").doc("rep_1").update({
      status: "resolved",
      updatedAt: pastDate(500),
    })
  );
});
