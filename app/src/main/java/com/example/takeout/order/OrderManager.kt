package com.example.takeout.order

import android.os.Looper
import com.example.takeout.data.Order
import com.example.takeout.data.User
import java.util.concurrent.*
import kotlin.random.Random

/**
 * 订单管理类，管理订单
 */

interface IOrderManager {

    fun addOrder(user: User): Boolean

    fun stop()

    fun start()

    fun getOrder(orderId: Long): Order?

    fun registerOrderManagerStateListener(listener: OrderStateListener)

    fun unRegisterOrderManagerStateListener(listener: OrderStateListener)

    fun registerOrderListener(listener: OrderListener)

    fun unRegisterOrderListener(listener: OrderListener)

    interface OrderStateListener {
        fun stateUpdate(state: OrderManager.State)
    }

    interface OrderListener {
        fun onOrderGenerated(order: Order)
    }
}

class OrderManager private constructor() : IOrderManager {

    companion object {
        val INSTANCE : IOrderManager by lazy { OrderManager() }
    }

    enum class State {
        RUNNING,
        STOPPED
    }

    private var state: State = State.RUNNING

    private val orders = ConcurrentHashMap<Long, Order>()

    private val orderManagerStateListeners = CopyOnWriteArrayList<IOrderManager.OrderStateListener>()

    private val orderListeners = CopyOnWriteArrayList<IOrderManager.OrderListener>()

    /**
     * 下单
     * 返回下单结果
     */
    override fun addOrder(user: User): Boolean {
        if (Thread.currentThread() == Looper.getMainLooper().thread) throw java.lang.IllegalStateException("ui线程不允许下单~")
        if (state == State.RUNNING) {
            val order = Order(user.userId, Random.nextLong(), Order.PLACE_ORDER, System.currentTimeMillis())
            orders[order.orderId] = order

            for (listener in orderListeners) listener.onOrderGenerated(order)
            return true
        }
        return false
    }

    override fun stop() {
        state = State.STOPPED
        for (listener in orderManagerStateListeners) listener.stateUpdate(state)
    }

    override fun start() {
        state = State.RUNNING
        for (listener in orderManagerStateListeners) listener.stateUpdate(state)
    }

    override fun getOrder(orderId: Long): Order? {
        return orders[orderId]
    }

    override fun registerOrderManagerStateListener(listener: IOrderManager.OrderStateListener) {
        orderManagerStateListeners.add(listener)
    }

    override fun unRegisterOrderManagerStateListener(listener: IOrderManager.OrderStateListener) {
        orderManagerStateListeners.remove(listener)
    }

    override fun registerOrderListener(listener: IOrderManager.OrderListener) {
        orderListeners.add(listener)
    }

    override fun unRegisterOrderListener(listener: IOrderManager.OrderListener) {
        orderListeners.remove(listener)
    }
}