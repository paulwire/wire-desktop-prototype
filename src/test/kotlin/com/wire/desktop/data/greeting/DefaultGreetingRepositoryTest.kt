package com.wire.desktop.data.greeting

import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultGreetingRepositoryTest {

    @Test
    fun `greeting returns the expected prototype name`() {
        val repository = DefaultGreetingRepository()

        val result = repository.greeting()

        assertEquals("Wire Desktop Prototype", result)
    }
}
