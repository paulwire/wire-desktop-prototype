package com.wire.desktop.presentation.main

import com.wire.desktop.data.greeting.GreetingRepository

class MainViewModel(private val greetingRepository: GreetingRepository) {

    fun loadGreeting(): String = greetingRepository.greeting()
}
