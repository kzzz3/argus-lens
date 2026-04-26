package com.kzzz3.argus.lens.app

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ArgusLensAppViewModel @Inject constructor(
    val dependencies: AppDependencies,
) : ViewModel()
