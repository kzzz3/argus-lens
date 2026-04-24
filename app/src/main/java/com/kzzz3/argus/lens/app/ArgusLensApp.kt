package com.kzzz3.argus.lens.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun ArgusLensApp() {
    val context = LocalContext.current
    val dependencies = rememberAppDependencies(context)
    AppRouteHost(dependencies = dependencies)
}
