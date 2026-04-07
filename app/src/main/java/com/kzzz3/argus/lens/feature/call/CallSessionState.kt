package com.kzzz3.argus.lens.feature.call

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

data class CallSessionState(
    val conversationId: String = "",
    val contactDisplayName: String = "",
    val mode: CallSessionMode = CallSessionMode.Audio,
    val status: CallSessionStatus = CallSessionStatus.Connecting,
    val durationLabel: String = "00:00",
    val isMuted: Boolean = false,
    val isSpeakerEnabled: Boolean = false,
    val isCameraEnabled: Boolean = true,
) {
    companion object {
        val Saver: Saver<CallSessionState, Any> = listSaver(
            save = { state ->
                listOf(
                    state.conversationId,
                    state.contactDisplayName,
                    state.mode.name,
                    state.status.name,
                    state.durationLabel,
                    state.isMuted,
                    state.isSpeakerEnabled,
                    state.isCameraEnabled,
                )
            },
            restore = { values ->
                if (values.size != 8) {
                    null
                } else {
                    CallSessionState(
                        conversationId = values[0] as String,
                        contactDisplayName = values[1] as String,
                        mode = CallSessionMode.valueOf(values[2] as String),
                        status = CallSessionStatus.valueOf(values[3] as String),
                        durationLabel = values[4] as String,
                        isMuted = values[5] as Boolean,
                        isSpeakerEnabled = values[6] as Boolean,
                        isCameraEnabled = values[7] as Boolean,
                    )
                }
            }
        )
    }
}

enum class CallSessionMode {
    Audio,
    Video,
}

enum class CallSessionStatus {
    Connecting,
    Active,
    Ended,
}
