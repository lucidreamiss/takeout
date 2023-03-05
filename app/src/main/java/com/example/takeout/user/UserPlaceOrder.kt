package com.example.takeout.user

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import com.example.takeout.data.User
import com.example.takeout.order.IOrderManager
import com.example.takeout.order.OrderManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

/**
 * 用户下单线程，模拟用户下单过程
 */
class UserPlaceOrder private constructor() {
    companion object {
        val INSTANCE by lazy { UserPlaceOrder() }

        const val MSG_START_ORDER = 1

        const val MSG_STOP_ORDER = 2
    }

    private val started = AtomicBoolean(false)

    private val handlerThread = HandlerThread("OrderGenerator-Thread").apply {
        start()
    }

    private val orderGenerator = object : Handler(handlerThread.looper) {
        override fun handleMessage(msg: Message) {
            when(msg.what) {
                MSG_START_ORDER -> {
                    while (started.get()) {
                        OrderManager.INSTANCE.addOrder(User(Random.nextLong()))
                        Thread.sleep(Random.nextLong(5000, 15000))
                    }
                }
            }
        }
    }

    private val orderManagerStateListener = object : IOrderManager.OrderStateListener {
        override fun stateUpdate(state: OrderManager.State) {
            if (state == OrderManager.State.RUNNING) start()
            else if (state == OrderManager.State.STOPPED) stop()
        }
    }

    init {
        OrderManager.INSTANCE.registerOrderManagerStateListener(orderManagerStateListener)
    }

    fun start() {
        if (!started.compareAndSet(false, true)) return
        orderGenerator.sendMessage(Message.obtain().apply { what = MSG_START_ORDER })
    }

    fun stop() {
        started.set(false)
        orderGenerator.sendMessage(Message.obtain().apply { what = MSG_STOP_ORDER })
    }
}