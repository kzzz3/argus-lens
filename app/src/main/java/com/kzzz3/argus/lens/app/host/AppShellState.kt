package com.kzzz3.argus.lens.app.host

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.app.runtime.AppRestorableEntryContext
import com.kzzz3.argus.lens.core.data.friend.FriendEntry
import com.kzzz3.argus.lens.core.data.friend.FriendRequestsSnapshot
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeConnectionState
import com.kzzz3.argus.lens.session.SessionCredentials
import com.kzzz3.argus.lens.feature.call.CallSessionState
import com.kzzz3.argus.lens.feature.contacts.ContactsState
import com.kzzz3.argus.lens.feature.contacts.FriendRequestStatusState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.model.session.AppSessionState

internal data class AppShellState(
    val appSessionState: AppSessionState,
    val currentRoute: AppRoute,
    val activeChatConversationId: String,
    val restorableEntryContext: AppRestorableEntryContext?,
    val hydratedConversationAccountId: String?,
    val realtimeConnectionState: ConversationRealtimeConnectionState,
    val realtimeLastEventId: String,
    val realtimeReconnectGeneration: Int,
)

internal data class AppShellCallbacks(
    val onRouteChanged: (AppRoute) -> Unit,
    val onConversationOpened: (String) -> Unit,
    val onActiveChatConversationChanged: (String) -> Unit,
    val onHydratedSessionApplied: (AppSessionState, String?) -> Unit,
    val onAuthenticatedSessionApplied: (AppSessionState, SessionCredentials, String, Int) -> Unit,
    val onSessionRefreshed: (AppSessionState) -> Unit,
    val onSessionCleared: () -> Unit,
    val onHydratedConversationAccountChanged: (String?) -> Unit,
    val onRestorableEntryContextCleared: () -> Unit,
    val onRealtimeConnectionStateChanged: (ConversationRealtimeConnectionState) -> Unit,
    val onRealtimeEventIdRecorded: (String) -> Unit,
    val onRealtimeLastEventIdReset: () -> Unit,
    val onRealtimeReconnectIncremented: () -> Unit,
)

internal data class AppFeatureCallbacks(
    val onCallSessionStateChanged: (CallSessionState) -> Unit,
    val onContactsStateChanged: (ContactsState) -> Unit,
    val onFriendsChanged: (List<FriendEntry>) -> Unit,
    val onChatStatusChanged: (String?, Boolean) -> Unit,
    val onChatStatusCleared: () -> Unit,
    val onFriendRequestStatusChanged: (FriendRequestStatusState) -> Unit,
    val onFriendRequestsSnapshotChanged: (FriendRequestsSnapshot) -> Unit,
    val onFriendRequestStatusReset: () -> Unit,
    val onConversationThreadsChanged: (ConversationThreadsState) -> Unit,
)
