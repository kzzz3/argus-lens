package com.kzzz3.argus.lens.app

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun ArgusLensApp(
    viewModel: ArgusLensAppViewModel = hiltViewModel(),
) {
    ArgusLensNavHost(dependencies = viewModel.dependencies)
}
