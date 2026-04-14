package com.kzzz3.argus.lens.data.conversation

fun buildDirectConversationId(
    currentAccountId: String,
    friendAccountId: String,
): String {
    val normalizedCurrent = currentAccountId.trim()
    val normalizedFriend = friendAccountId.trim()
    if (normalizedCurrent.isEmpty() || normalizedFriend.isEmpty()) {
        return normalizedFriend
    }
    return if (normalizedCurrent < normalizedFriend) {
        "conv-direct-$normalizedCurrent-$normalizedFriend"
    } else {
        "conv-direct-$normalizedFriend-$normalizedCurrent"
    }
}
