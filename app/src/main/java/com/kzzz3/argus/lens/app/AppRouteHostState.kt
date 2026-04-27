package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.data.friend.FriendEntry
import com.kzzz3.argus.lens.data.friend.FriendRequestsSnapshot
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeConnectionState
import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.feature.call.CallSessionState
import com.kzzz3.argus.lens.feature.contacts.ContactsState
import com.kzzz3.argus.lens.feature.contacts.FriendRequestStatusState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.model.session.AppSessionState

internal data class AppRouteHostState(
    val appSessionState: AppSessionState,
    val conversationThreadsState: ConversationThreadsState,
    val currentRoute: AppRoute,
    val callSessionState: CallSessionState,
    val contactsState: ContactsState,
    val friends: List<FriendEntry>,
    val selectedConversationId: String,
    val restorableEntryContext: AppRestorableEntryContext?,
    val chatStatusMessage: String?,
    val chatStatusError: Boolean,
    val friendRequestsSnapshot: FriendRequestsSnapshot,
    val friendRequestsStatusMessage: String?,
    val friendRequestsStatusError: Boolean,
    val hydratedConversationAccountId: String?,
    val realtimeConnectionState: ConversationRealtimeConnectionState,
    val realtimeLastEventId: String,
    val realtimeReconnectGeneration: Int,
)

internal data class AppRouteHostCallbacks(
    val onRouteChanged: (AppRoute) -> Unit,
    val onCallSessionStateChanged: (CallSessionState) -> Unit,
    val onContactsStateChanged: (ContactsState) -> Unit,
    val onFriendsChanged: (List<FriendEntry>) -> Unit,
    val onConversationOpened: (String) -> Unit,
    val onSelectedConversationChanged: (String) -> Unit,
    val onChatStatusChanged: (String?, Boolean) -> Unit,
    val onChatStatusCleared: () -> Unit,
    val onFriendRequestStatusChanged: (FriendRequestStatusState) -> Unit,
    val onFriendRequestsSnapshotChanged: (FriendRequestsSnapshot) -> Unit,
    val onFriendRequestStatusReset: () -> Unit,
    val onHydratedSessionApplied: (AppSessionState, String?) -> Unit,
    val onAuthenticatedSessionApplied: (AppSessionState, SessionCredentials, String, Int) -> Unit,
    val onSessionRefreshed: (AppSessionState) -> Unit,
    val onSessionCleared: () -> Unit,
    val onConversationThreadsChanged: (ConversationThreadsState) -> Unit,
    val onHydratedConversationAccountChanged: (String?) -> Unit,
    val onRestorableEntryContextCleared: () -> Unit,
    val onRealtimeConnectionStateChanged: (ConversationRealtimeConnectionState) -> Unit,
    val onRealtimeEventIdRecorded: (String) -> Unit,
    val onRealtimeLastEventIdReset: () -> Unit,
    val onRealtimeReconnectIncremented: () -> Unit,
)
