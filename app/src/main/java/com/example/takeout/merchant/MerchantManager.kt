package com.example.takeout.merchant

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import com.example.takeout.data.Food
import com.example.takeout.data.Order
import com.example.takeout.order.IOrderManager
import com.example.takeout.order.OrderManager
import com.example.takeout.user.UserPlaceOrder
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean


/**
 * 商家
 *
 */
class MerchantManager private constructor() {

    companion object {
        const val ROW = 4
        const val COL = 4

        const val MSG_START_COOKING = 1

        const val MSG_STOP_COOKING = 2

        val INSTANCE by lazy { MerchantManager() }
    }

    private val waitForCookingQueue = LinkedBlockingQueue<Food>()

    val shelf = Shelf(ROW, COL)

    fun init() {
        // nothing
    }

    private val orderListener = object : IOrderManager.OrderListener {
        override fun onOrderGenerated(order: Order) {
            waitForCookingQueue.offer(Food(order.orderId, System.currentTimeMillis()))
        }
    }

    private val shelfCountObserver = object : ShelfObserver() {

        override fun onFoodAdded(row: Int, col: Int, food: Food) {
            // 货架已满
            if (shelf.isFull()) {
                // stop cooking
                cookingHandler.cooking.compareAndSet(true, false)
                // stop order
                OrderManager.INSTANCE.stop()
            }
        }

        override fun onFoodRemoved(row: Int, col: Int, food: Food) {
            // 货架出现空余位置，若当前停止制作，那么重新开始
            if (!cookingHandler.cooking.get()) {
                cookingHandler.sendMessage(Message.obtain().apply { what = MSG_START_COOKING })
                // start order
                OrderManager.INSTANCE.start()
            }
        }
    }

    private val handlerThread = HandlerThread("Cooking-Thread").apply {
        start()
    }

    private val cookingHandler = object : Handler(handlerThread.looper) {

        @Volatile
        var cooking = AtomicBoolean(false)

        override fun handleMessage(msg: Message) {
            when(msg.what) {
                MSG_START_COOKING -> {
                    if (cooking.compareAndSet(false, true)) {
                        while (cooking.get()) {
                            val food = waitForCookingQueue.take()
                            // cooking
                            Thread.sleep(3000)
                            shelf.addFood(food)
                        }
                    }
                }
            }
        }
    }

    init {
        OrderManager.INSTANCE.registerOrderListener(orderListener)
        shelf.subscribe(shelfCountObserver)
        // 开始等待制作
        cookingHandler.sendMessage(Message.obtain().apply { what = MSG_START_COOKING })
    }

    class Shelf(val row: Int, val col: Int) {

        val shelf = Array<Array<Food?>>(row) { Array(col) { null } }

        private val shelfObservers = CopyOnWriteArrayList<ShelfObserver>()

        @Volatile
        private var count = 0

        fun addFood(food: Food): Boolean {
            synchronized(shelf) {
                for (i in 0 until  row) {
                    for (j in 0 until col ) {
                        if (shelf[i][j] == null) {
                            shelf[i][j] = food
                            count++
                            shelfObservers.map { it.onFoodAdded(i, j, food) }
                            Log.d("litao.lucky", "addFood $count")
                            return true
                        }
                    }
                }
            }

            throw java.lang.IllegalStateException("货架已满")
        }

        fun removeFood(row: Int, col: Int, food: Food): Boolean {
            synchronized(shelf) {
                shelf[row][col] = null
                count--
                shelfObservers.map { it.onFoodRemoved(row, col, food) }
                Log.d("litao.lucky", "removeFood $count")
            }

            return true
        }

        fun isFull(): Boolean {
            return count == ROW * COL
        }

        fun isEmpty(): Boolean {
            return count == 0
        }

        fun subscribe(observer: ShelfObserver) {
            shelfObservers.add(observer)
        }

        fun unSubscribe(observer: ShelfObserver) {
            shelfObservers.remove(observer)
        }
    }

    abstract class ShelfObserver {
        open fun onFoodAdded(row: Int, col: Int, food: Food) {}

        open fun onFoodRemoved(row: Int, col: Int, food: Food) {}
    }
}