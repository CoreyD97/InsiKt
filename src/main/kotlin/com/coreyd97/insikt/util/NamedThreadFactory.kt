package com.coreyd97.insikt.util

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

class NamedThreadFactory(private val name: String?) : ThreadFactory {
    private val atomicInteger: AtomicInteger

    init {
        this.atomicInteger = AtomicInteger(0)
    }

    override fun newThread(r: Runnable?): Thread {
        return Thread(
            r,
            String.format("%s-Thread-%d", this.name, this.atomicInteger.incrementAndGet())
        )
    }
}
