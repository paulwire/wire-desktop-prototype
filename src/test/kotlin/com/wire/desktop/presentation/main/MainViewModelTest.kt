package com.wire.desktop.presentation.main

import com.wire.desktop.data.greeting.GreetingRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class MainViewModelTest {

    @Test
    fun `loadGreeting returns the value from the repository`() {
        val repository = object : GreetingRepository {
            override fun greeting(): String = "Hello from fake"
        }
        val viewModel = MainViewModel(repository)

        val result = viewModel.loadGreeting()

        assertEquals("Hello from fake", result)
    }
}
