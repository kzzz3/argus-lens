package com.kzzz3.argus.lens.feature.call

data class CallSessionUiState(
    val title: String,
    val subtitle: String,
    val modeLabel: String,
    val statusLabel: String,
    val durationLabel: String,
    val muteActionLabel: String,
    val speakerActionLabel: String,
    val cameraActionLabel: String,
    val endCallActionLabel: String,
    val isCameraActionVisible: Boolean,
)
