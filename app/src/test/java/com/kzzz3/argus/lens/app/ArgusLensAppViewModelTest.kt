package com.kzzz3.argus.lens.app

import androidx.lifecycle.ViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

class ArgusLensAppViewModelTest {
    @Test
    fun appViewModelUsesInjectedDependenciesWithoutApplicationSuperclass() {
        assertEquals(ViewModel::class.java, ArgusLensAppViewModel::class.java.superclass)
    }
}
