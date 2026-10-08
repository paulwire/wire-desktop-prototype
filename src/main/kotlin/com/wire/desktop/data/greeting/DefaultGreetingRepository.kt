package com.wire.desktop.data.greeting

class DefaultGreetingRepository : GreetingRepository {
    override fun greeting(): String = "Wire Desktop Prototype"
}
