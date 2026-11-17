package com.example.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DynamicFeed
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.core.AppLanguage
import com.example.core.AuthProvider
import com.example.core.EnglishStrings
import com.example.core.HindiStrings
import com.example.core.LocalAuthController
import com.example.core.LocalStrings
import com.example.core.PreferencesStore
import com.example.core.ThemeMode
import com.example.core.UiState
import com.example.data.CommerceRepository
import com.example.data.SocialRepository
import com.example.data.UserRepository
import com.example.models.UserProfile
import com.example.ui.components.CityPickerSheet
import com.example.ui.components.EmptyStateCard
import com.example.ui.screens.AdminModerationScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatThreadScreen
import com.example.ui.screens.CreateScreen
import com.example.ui.screens.EventsScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.MarketplaceScreen
import com.example.ui.screens.MessagesInboxScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SubscriptionScreen
import com.example.ui.theme.CityLoopTheme
import com.example.viewmodel.AiViewModel
import com.example.viewmodel.AuthStatus
import com.example.viewmodel.CommerceViewModel
import com.example.viewmodel.FeedViewModel
import com.example.viewmodel.SessionViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

object CityLoopRoutes {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val EVENTS = "events"
    const val CREATE = "create"
    const val MARKETPLACE = "marketplace"
    const val PROFILE = "profile"
    const val MESSAGES = "messages"
    const val CHAT = "chat"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"
    const val ADMIN = "admin"
    const val PLUS = "plus"
}

private data class BottomTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun CityLoopApp() {
    val context = LocalContext.current
    val prefs = remember(context) { PreferencesStore(context.applicationContext) }
    val appLanguage by prefs.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.ENGLISH)
    val themeMode by prefs.themeModeFlow.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val strings = if (appLanguage == AppLanguage.HINDI) HindiStrings else EnglishStrings
    val scope = rememberCoroutineScope()

    CompositionLocalProvider(LocalStrings provides strings) {
        CityLoopTheme(themeMode = themeMode) {
            AuthProvider {
                val auth = LocalAuthController.current
                when (val status = auth.state.status) {
                    AuthStatus.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    AuthStatus.Unauthenticated -> {
                        AuthScreen(
                            currentLanguage = appLanguage,
                            onLanguageChange = { lang -> scope.launch { prefs.setLanguage(lang) } }
                        )
                    }

                    is AuthStatus.Authenticated -> {
                        val uid = status.user.uid
                        key(uid) {
                            AuthenticatedCityLoop(
                                currentUserId = uid,
                                prefs = prefs,
                                onSignOut = { auth.signOut {} }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedCityLoop(
    currentUserId: String,
    prefs: PreferencesStore,
    onSignOut: () -> Unit
) {
    val sessionVm: SessionViewModel = viewModel(
        key = "session_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                SessionViewModel(UserRepository(db), prefs, currentUserId)
            }
        }
    )

    val profileState by sessionVm.profileState.collectAsStateWithLifecycle()
    val privateSettings by sessionVm.privateSettings.collectAsStateWithLifecycle()
    val activeCity by sessionVm.selectedCity.collectAsStateWithLifecycle()
    val appLanguage by sessionVm.language.collectAsStateWithLifecycle()
    val themeMode by sessionVm.themeMode.collectAsStateWithLifecycle()
    val sessionBusy by sessionVm.isBusy.collectAsStateWithLifecycle()
    val sessionStatus by sessionVm.actionStatus.collectAsStateWithLifecycle()

    when (val state = profileState) {
        is UiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is UiState.Error -> {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                EmptyStateCard(
                    icon = Icons.Rounded.Person,
                    title = "Could not load your CITYLOOP profile",
                    subtitle = state.message,
                    ctaLabel = "Sign Out",
                    onCtaClick = onSignOut
                )
            }
        }

        is UiState.Success -> {
            val profile = state.data
            if (profile == null || !profile.onboardingDone) {
                OnboardingScreen(
                    isBusy = sessionBusy,
                    errorMessage = sessionStatus,
                    onCompleteOnboarding = { name, handle, city, neighborhood, bio, emoji, interests ->
                        sessionVm.completeOnboarding(name, handle, city, neighborhood, bio, emoji, interests)
                    }
                )
            } else {
                MainAuthenticatedScaffold(
                    currentUserId = currentUserId,
                    profile = profile,
                    activeCity = activeCity,
                    appLanguage = appLanguage,
                    themeMode = themeMode,
                    privateSettings = privateSettings,
                    sessionVm = sessionVm,
                    onSignOut = onSignOut
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAuthenticatedScaffold(
    currentUserId: String,
    profile: UserProfile,
    activeCity: String,
    appLanguage: AppLanguage,
    themeMode: ThemeMode,
    privateSettings: com.example.models.UserPrivateSettings?,
    sessionVm: SessionViewModel,
    onSignOut: () -> Unit
) {
    val strings = LocalStrings.current
    val isAdmin = sessionVm.isAdminUser

    val feedVm: FeedViewModel = viewModel(
        key = "feed_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                FeedViewModel(SocialRepository(db), sessionVm.selectedCity, currentUserId, isAdmin)
            }
        }
    )

    val commerceVm: CommerceViewModel = viewModel(
        key = "commerce_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                CommerceViewModel(CommerceRepository(db), sessionVm.selectedCity, currentUserId)
            }
        }
    )

    val aiVm: AiViewModel = viewModel(key = "ai_$currentUserId")

    val postsState by feedVm.postsState.collectAsStateWithLifecycle()
    val myPostsState by feedVm.myPostsState.collectAsStateWithLifecycle()
    val commentsState by feedVm.activeCommentsState.collectAsStateWithLifecycle()
    val communitiesState by feedVm.communitiesState.collectAsStateWithLifecycle()
    val reportsState by feedVm.reportsState.collectAsStateWithLifecycle()
    val likedPostIds by feedVm.likedPostIds.collectAsStateWithLifecycle()
    val joinedCommunityIds by feedVm.joinedCommunityIds.collectAsStateWithLifecycle()
    val feedSearchQuery by feedVm.searchQuery.collectAsStateWithLifecycle()
    val isRefreshing by feedVm.isRefreshing.collectAsStateWithLifecycle()

    val listingsState by commerceVm.listingsState.collectAsStateWithLifecycle()
    val conversationsState by commerceVm.conversationsState.collectAsStateWithLifecycle()
    val activeConversation by commerceVm.activeConversation.collectAsStateWithLifecycle()
    val activeMessagesState by commerceVm.activeMessagesState.collectAsStateWithLifecycle()
    val isSendingMessage by commerceVm.isSendingMessage.collectAsStateWithLifecycle()
    val eventsState by commerceVm.eventsState.collectAsStateWithLifecycle()
    val rsvpedEventIds by commerceVm.rsvpedEventIds.collectAsStateWithLifecycle()
    val pendingRsvpIds by commerceVm.pendingRsvpIds.collectAsStateWithLifecycle()
    val isRefreshingEvents by commerceVm.isRefreshingEvents.collectAsStateWithLifecycle()
    val businessesState by commerceVm.businessesState.collectAsStateWithLifecycle()

    val neighbors by sessionVm.cityNeighbors.collectAsStateWithLifecycle()
    val following by sessionVm.following.collectAsStateWithLifecycle()
    val savedItems by sessionVm.savedItems.collectAsStateWithLifecycle()
    val notifications by sessionVm.notifications.collectAsStateWithLifecycle()
    val customAvatarUri by sessionVm.customAvatarUri.collectAsStateWithLifecycle()

    val conciergeState by aiVm.conciergeState.collectAsStateWithLifecycle()
    val aiDraftBusy by aiVm.draftAssistLoading.collectAsStateWithLifecycle()
    val translatedMap by aiVm.translatedMap.collectAsStateWithLifecycle()
    val translatingId by aiVm.translatingId.collectAsStateWithLifecycle()

    val sessionMsg by sessionVm.actionStatus.collectAsStateWithLifecycle()
    val feedMsg by feedVm.feedback.collectAsStateWithLifecycle()
    val commerceMsg by commerceVm.feedback.collectAsStateWithLifecycle()
    val aiMsg by aiVm.aiError.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(sessionMsg, feedMsg, commerceMsg, aiMsg) {
        val msg = sessionMsg ?: feedMsg ?: commerceMsg ?: aiMsg
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            sessionVm.clearStatus()
            feedVm.clearFeedback()
            commerceVm.clearFeedback()
            aiVm.clearAiError()
        }
    }

    val uiScope = rememberCoroutineScope()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: CityLoopRoutes.HOME

    val bottomTabs = listOf(
        BottomTab(CityLoopRoutes.HOME, strings.navFeed, Icons.Rounded.DynamicFeed, Icons.Outlined.DynamicFeed),
        BottomTab(CityLoopRoutes.EXPLORE, strings.navExplore, Icons.Rounded.Explore, Icons.Outlined.Explore),
        BottomTab(CityLoopRoutes.EVENTS, strings.navEvents, Icons.Rounded.Event, Icons.Outlined.Event),
        BottomTab(CityLoopRoutes.PROFILE, strings.navProfile, Icons.Rounded.Person, Icons.Outlined.Person)
    )
    val isTopLevelRoute = bottomTabs.any { it.route == currentRoute }
    val unreadNotifCount = notifications.count { !it.readFlag }
    val savedTargetIds = remember(savedItems) { savedItems.map { it.targetId }.toSet() }
    val followingIds = remember(following) { following.map { it.targetUserId }.toSet() }
    var showCityPicker by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (isTopLevelRoute) {
                    TopAppBar(
                        title = {
                            Surface(
                                onClick = { showCityPicker = true },
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                modifier = Modifier.testTag("city_picker_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text(" $activeCity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Switch city", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { navController.navigate(CityLoopRoutes.MARKETPLACE) },
                                modifier = Modifier.testTag("topbar_marketplace_button")
                            ) {
                                Icon(Icons.Rounded.Storefront, contentDescription = strings.navMarket)
                            }
                            IconButton(
                                onClick = { navController.navigate(CityLoopRoutes.MESSAGES) },
                                modifier = Modifier.testTag("topbar_messages_button")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = "Messages")
                            }
                            IconButton(
                                onClick = { navController.navigate(CityLoopRoutes.NOTIFICATIONS) },
                                modifier = Modifier.testTag("topbar_notifications_button")
                            ) {
                                BadgedBox(badge = {
                                    if (unreadNotifCount > 0) Badge { Text("$unreadNotifCount") }
                                }) {
                                    Icon(Icons.Rounded.Notifications, contentDescription = "Notifications")
                                }
                            }
                        }
                    )
                } else if (currentRoute == CityLoopRoutes.CREATE || currentRoute == CityLoopRoutes.MARKETPLACE) {
                    TopAppBar(
                        title = {
                            Text(
                                if (currentRoute == CityLoopRoutes.CREATE) strings.navCreate else strings.navMarket
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.testTag("secondary_topbar_back")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            },
            floatingActionButton = {
                if (isTopLevelRoute && currentRoute != CityLoopRoutes.HOME && currentRoute != CityLoopRoutes.PROFILE) {
                    FloatingActionButton(
                        onClick = { navController.navigate(CityLoopRoutes.CREATE) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_create")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = strings.navCreate)
                    }
                }
            },
            bottomBar = {
                if (isTopLevelRoute && !useRail) {
                    NavigationBar(windowInsets = WindowInsets.navigationBars) {
                        bottomTabs.forEach { tab ->
                            val selected = backStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(if (selected) tab.selectedIcon else tab.unselectedIcon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (isTopLevelRoute && useRail) {
                    NavigationRail {
                        bottomTabs.forEach { tab ->
                            val selected = backStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(if (selected) tab.selectedIcon else tab.unselectedIcon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_${tab.route}")
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = CityLoopRoutes.HOME,
                    modifier = Modifier.weight(1f).fillMaxSize()
                ) {
                    composable(CityLoopRoutes.HOME) {
                        FeedScreen(
                            activeCity = activeCity,
                            profile = profile,
                            postsState = postsState,
                            commentsState = commentsState,
                            likedPostIds = likedPostIds,
                            savedTargetIds = savedTargetIds,
                            translatedMap = translatedMap,
                            translatingId = translatingId,
                            currentLanguage = appLanguage,
                            searchQuery = feedSearchQuery,
                            onSearchQueryChange = { feedVm.updateSearchQuery(it) },
                            isRefreshing = isRefreshing,
                            aiBusy = aiDraftBusy,
                            onRefresh = { feedVm.triggerRefresh() },
                            onPolishWithAi = { draft, type, cb ->
                                aiVm.polishDraft(draft, type, activeCity, cb)
                            },
                            onPublishPost = { postType, caption, area, mediaUri, tagsRaw, onSuccess, onError ->
                                feedVm.createPost(
                                    profile = profile.copy(city = activeCity),
                                    postType = postType,
                                    caption = caption,
                                    neighborhood = area,
                                    mediaUri = mediaUri,
                                    videoDurationSec = 0L,
                                    tagsRaw = tagsRaw,
                                    onSuccess = onSuccess,
                                    onError = onError
                                )
                            },
                            onToggleLike = { feedVm.toggleLike(it, profile.displayName) },
                            onOpenComments = { feedVm.openComments(it.id) },
                            onDismissComments = { feedVm.openComments(null) },
                            onAddComment = { post, body, parentId -> feedVm.addComment(post, profile, body, parentId) },
                            onToggleSave = { post ->
                                sessionVm.toggleSave("post", post.id, post.caption.take(80), "By ${post.authorName}", post.city)
                            },
                            onTranslatePost = { post ->
                                val target = if (appLanguage == AppLanguage.HINDI) "English" else "Hindi"
                                aiVm.translateItem(post.id, post.caption, target)
                            },
                            onDeletePost = { feedVm.deletePost(it) },
                            onMessageAuthor = { uid, name, ctx ->
                                commerceVm.startChatWith(profile, uid, name, ctx) {
                                    navController.navigate(CityLoopRoutes.CHAT)
                                }
                            },
                            onBlockAuthor = { targetId -> sessionVm.toggleBlockUser(targetId, profile.blockedUserIds) },
                            onReportPost = { targetId, reason -> feedVm.reportContent("post", targetId, reason, activeCity) }
                        )
                    }

                    composable(CityLoopRoutes.EXPLORE) {
                        ExploreScreen(
                            activeCity = activeCity,
                            profile = profile,
                            neighbors = neighbors,
                            followingIds = followingIds,
                            communitiesState = communitiesState,
                            joinedCommunityIds = joinedCommunityIds,
                            eventsState = eventsState,
                            rsvpedEventIds = rsvpedEventIds,
                            businessesState = businessesState,
                            conciergeState = conciergeState,
                            currentLanguage = appLanguage,
                            onToggleFollow = { target -> sessionVm.toggleFollow(target, profile.displayName) },
                            onMessageUser = { uid, name, ctx ->
                                commerceVm.startChatWith(profile, uid, name, ctx) {
                                    navController.navigate(CityLoopRoutes.CHAT)
                                }
                            },
                            onToggleJoinCommunity = { comm -> feedVm.toggleJoinCommunity(comm, profile) },
                            onToggleEventRsvp = { evt -> commerceVm.toggleRsvp(evt, profile) },
                            onSaveEvent = { evt ->
                                sessionVm.toggleSave("event", evt.id, evt.title, "${evt.dateLabel} • ${evt.venue}", evt.city)
                            },
                            onAskConcierge = { q ->
                                aiVm.askCityConcierge(activeCity, profile.neighborhood, q, appLanguage.label)
                            },
                            onNavigateToCreate = { navController.navigate(CityLoopRoutes.CREATE) },
                            onShowMessage = { msg ->
                                uiScope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        )
                    }

                    composable(CityLoopRoutes.EVENTS) {
                        EventsScreen(
                            activeCity = activeCity,
                            profile = profile,
                            eventsState = eventsState,
                            rsvpedEventIds = rsvpedEventIds,
                            pendingRsvpIds = pendingRsvpIds,
                            savedTargetIds = savedTargetIds,
                            currentLanguage = appLanguage,
                            isRefreshing = isRefreshingEvents,
                            onRefresh = { commerceVm.triggerEventsRefresh() },
                            onToggleEventRsvp = { evt -> commerceVm.toggleRsvp(evt, profile) },
                            onSaveEvent = { evt ->
                                sessionVm.toggleSave("event", evt.id, evt.title, "${evt.dateLabel} • ${evt.venue}", evt.city)
                            },
                            onMessageOrganizer = { uid, name, ctx ->
                                commerceVm.startChatWith(profile, uid, name, ctx) {
                                    navController.navigate(CityLoopRoutes.CHAT)
                                }
                            },
                            onNavigateToCreate = { navController.navigate(CityLoopRoutes.CREATE) }
                        )
                    }

                    composable(CityLoopRoutes.CREATE) {
                        val communities = (communitiesState as? UiState.Success)?.data ?: emptyList()
                        CreateScreen(
                            profile = profile.copy(city = activeCity),
                            communities = communities,
                            aiBusy = aiDraftBusy,
                            onPolishWithAi = { draft, type, cb ->
                                aiVm.polishDraft(draft, type, activeCity, cb)
                            },
                            onCreatePost = { type, caption, area, uri, dur, tags, commId, commName ->
                                feedVm.createPost(profile.copy(city = activeCity), type, caption, area, uri, dur, tags, commId, commName) {
                                    navController.navigate(CityLoopRoutes.HOME) { launchSingleTop = true }
                                }
                            },
                            onCreateListing = { title, desc, price, cat, cond, area, uri ->
                                commerceVm.createListing(profile.copy(city = activeCity), title, desc, price, cat, cond, area, uri) {
                                    navController.navigate(CityLoopRoutes.MARKETPLACE) { launchSingleTop = true }
                                }
                            },
                            onCreateCommunity = { name, desc, cat, emoji ->
                                feedVm.createCommunity(profile.copy(city = activeCity), name, desc, cat, emoji) {
                                    navController.navigate(CityLoopRoutes.EXPLORE) { launchSingleTop = true }
                                }
                            },
                            onCreateEvent = { title, desc, venue, date, cat, price ->
                                commerceVm.createEvent(profile.copy(city = activeCity), title, desc, venue, date, cat, price) {
                                    navController.navigate(CityLoopRoutes.EVENTS) { launchSingleTop = true }
                                }
                            },
                            onCreateBusiness = { name, cat, desc, addr, offerTitle, offerCode, discount ->
                                commerceVm.createBusiness(profile.copy(city = activeCity), name, cat, desc, addr, offerTitle, offerCode, discount) {
                                    navController.navigate(CityLoopRoutes.EXPLORE) { launchSingleTop = true }
                                }
                            }
                        )
                    }

                    composable(CityLoopRoutes.MARKETPLACE) {
                        MarketplaceScreen(
                            activeCity = activeCity,
                            profile = profile,
                            listingsState = listingsState,
                            savedTargetIds = savedTargetIds,
                            translatedMap = translatedMap,
                            translatingId = translatingId,
                            currentLanguage = appLanguage,
                            onNavigateToCreate = { navController.navigate(CityLoopRoutes.CREATE) },
                            onMessageSeller = { uid, name, ctx ->
                                commerceVm.startChatWith(profile, uid, name, ctx) {
                                    navController.navigate(CityLoopRoutes.CHAT)
                                }
                            },
                            onMarkSold = { commerceVm.markListingSold(it) },
                            onToggleSave = { item ->
                                sessionVm.toggleSave("listing", item.id, item.title, "₹${item.priceInr} • ${item.sellerName}", item.city)
                            },
                            onTranslateListing = { item ->
                                val target = if (appLanguage == AppLanguage.HINDI) "English" else "Hindi"
                                aiVm.translateItem(item.id, "${item.title}: ${item.description}", target)
                            },
                            onReportListing = { id, reason -> feedVm.reportContent("listing", id, reason, activeCity) }
                        )
                    }

                    composable(CityLoopRoutes.PROFILE) {
                        ProfileScreen(
                            profile = profile,
                            isAdminUser = isAdmin,
                            unreadNotificationsCount = unreadNotifCount,
                            myPostsState = myPostsState,
                            savedItems = savedItems,
                            following = following,
                            customAvatarUri = customAvatarUri,
                            currentLanguage = appLanguage,
                            onUpdateProfile = { name, bio, city, area, emoji ->
                                sessionVm.updateProfile(name, bio, city, area, emoji)
                            },
                            onUpdateBio = { newBio ->
                                sessionVm.updateBio(newBio, profile)
                            },
                            onUpdateAvatarUri = { uri ->
                                sessionVm.updateCustomAvatarUri(uri)
                            },
                            onDeletePost = { postId -> feedVm.deletePost(postId) },
                            onCreatePost = { navController.navigate(CityLoopRoutes.CREATE) },
                            onOpenMessages = { navController.navigate(CityLoopRoutes.MESSAGES) },
                            onOpenNotifications = { navController.navigate(CityLoopRoutes.NOTIFICATIONS) },
                            onOpenSettings = { navController.navigate(CityLoopRoutes.SETTINGS) },
                            onOpenPlus = { navController.navigate(CityLoopRoutes.PLUS) },
                            onOpenAdmin = { navController.navigate(CityLoopRoutes.ADMIN) }
                        )
                    }

                    composable(CityLoopRoutes.MESSAGES) {
                        MessagesInboxScreen(
                            currentUserId = currentUserId,
                            activeCity = activeCity,
                            blockedUserIds = profile.blockedUserIds,
                            neighbors = neighbors,
                            conversationsState = conversationsState,
                            currentLanguage = appLanguage,
                            onBack = { navController.popBackStack() },
                            onSelectConversation = { conv ->
                                commerceVm.selectConversation(conv)
                                navController.navigate(CityLoopRoutes.CHAT)
                            },
                            onStartNewChat = { neighbor ->
                                commerceVm.startChatWith(
                                    myProfile = profile,
                                    otherUserId = neighbor.userId,
                                    otherUserName = neighbor.displayName,
                                    contextTitle = "Direct chat in $activeCity"
                                ) {
                                    navController.navigate(CityLoopRoutes.CHAT)
                                }
                            }
                        )
                    }

                    composable(CityLoopRoutes.CHAT) {
                        val conv = activeConversation
                        if (conv == null) {
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        } else {
                            val liveConv = (conversationsState as? UiState.Success)
                                ?.data
                                ?.firstOrNull { it.id == conv.id }
                                ?: conv
                            ChatThreadScreen(
                                currentUserId = currentUserId,
                                conversation = liveConv,
                                messagesState = activeMessagesState,
                                isSending = isSendingMessage,
                                currentLanguage = appLanguage,
                                onBack = { navController.popBackStack() },
                                onSendMessage = { text, onSent ->
                                    commerceVm.sendMessage(liveConv, profile.displayName, text, onSent)
                                }
                            )
                        }
                    }

                    composable(CityLoopRoutes.NOTIFICATIONS) {
                        NotificationsScreen(
                            notifications = notifications,
                            onMarkRead = { sessionVm.markNotificationRead(it) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(CityLoopRoutes.SETTINGS) {
                        SettingsScreen(
                            profile = profile,
                            privateSettings = privateSettings,
                            currentLanguage = appLanguage,
                            currentTheme = themeMode,
                            onLanguageChange = { sessionVm.setLanguage(it) },
                            onThemeChange = { sessionVm.setThemeMode(it) },
                            onPrivacyChange = { push, priv -> sessionVm.updatePrivacy(push, priv) },
                            onUnblockUser = { targetId -> sessionVm.toggleBlockUser(targetId, profile.blockedUserIds) },
                            onSignOut = onSignOut,
                            onDeleteAccount = {
                                sessionVm.deleteAccount { onSignOut() }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(CityLoopRoutes.ADMIN) {
                        AdminModerationScreen(
                            reportsState = reportsState,
                            postsState = postsState,
                            businessesState = businessesState,
                            onResolveReport = { id, status -> feedVm.resolveReport(id, status) },
                            onModeratePost = { id, status, note -> feedVm.moderatePost(id, status, note) },
                            onVerifyBusiness = { id, verified -> commerceVm.verifyBusiness(id, verified) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(CityLoopRoutes.PLUS) {
                        SubscriptionScreen(
                            currentPlanTier = profile.planTier,
                            onSelectPlan = { tier -> sessionVm.setPlanTier(tier) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    if (showCityPicker) {
        CityPickerSheet(
            currentCity = activeCity,
            onCitySelected = { sessionVm.selectCity(it) },
            onDismiss = { showCityPicker = false }
        )
    }
}
