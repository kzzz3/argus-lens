package com.kzzz3.argus.lens.feature.call

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CallSessionState(
    val conversationId: String = "",
    val contactDisplayName: String = "",
    val mode: CallSessionMode = CallSessionMode.Audio,
    val status: CallSessionStatus = CallSessionStatus.Connecting,
    val durationLabel: String = "00:00",
    val isMuted: Boolean = false,
    val isSpeakerEnabled: Boolean = false,
    val isCameraEnabled: Boolean = true,
) : Parcelable

enum class CallSessionMode {
    Audio,
    Video,
}

enum class CallSessionStatus {
    Connecting,
    Active,
    Ended,
}
