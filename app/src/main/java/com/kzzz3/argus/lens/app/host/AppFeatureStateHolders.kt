package com.kzzz3.argus.lens.app.host

import com.kzzz3.argus.lens.feature.auth.AuthStateHolder
import com.kzzz3.argus.lens.feature.call.CallSessionStateHolder
import com.kzzz3.argus.lens.feature.contacts.ContactsFeatureStateHolder
import com.kzzz3.argus.lens.feature.inbox.ChatStateHolder
import com.kzzz3.argus.lens.feature.inbox.InboxChatFeatureStateHolder
import com.kzzz3.argus.lens.feature.inbox.InboxStateHolder
import com.kzzz3.argus.lens.feature.wallet.WalletStateHolder

internal data class AppFeatureStateHolders(
    val authStateHolder: AuthStateHolder,
    val contactsFeatureStateHolder: ContactsFeatureStateHolder,
    val callSessionStateHolder: CallSessionStateHolder,
    val inboxChatFeatureStateHolder: InboxChatFeatureStateHolder,
    val chatStateHolder: ChatStateHolder,
    val inboxStateHolder: InboxStateHolder,
    val walletStateHolder: WalletStateHolder,
)

internal fun AppFeatureStateHolders.asFeatureCallbacks(): AppFeatureCallbacks {
    return AppFeatureCallbacks(
        onCallSessionStateChanged = callSessionStateHolder::replaceState,
        onContactsStateChanged = contactsFeatureStateHolder::replaceContactsState,
        onFriendsChanged = contactsFeatureStateHolder::replaceFriends,
        onChatStatusChanged = inboxChatFeatureStateHolder::updateChatStatus,
        onChatStatusCleared = inboxChatFeatureStateHolder::clearChatStatus,
        onFriendRequestStatusChanged = contactsFeatureStateHolder::applyFriendRequestStatus,
        onFriendRequestsSnapshotChanged = contactsFeatureStateHolder::replaceFriendRequestsSnapshot,
        onFriendRequestStatusReset = contactsFeatureStateHolder::resetFriendRequestStatus,
        onConversationThreadsChanged = inboxChatFeatureStateHolder::replaceConversationThreadsState,
    )
}
