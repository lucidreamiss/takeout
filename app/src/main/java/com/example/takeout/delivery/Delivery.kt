package com.example.takeout.delivery

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import com.example.takeout.data.Food
import com.example.takeout.merchant.MerchantManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random


/**
 * 骑手这块写不动了, 两分钟必须送出去的那个逻辑没写
 */
class Delivery  private constructor() {

    companion object {
        const val MSG_START = 1
        val INSTANCE by lazy { Delivery() }
    }

    private val handlerThread = HandlerThread("Delivery-Thread").apply {
        start()
    }

    private val delivery = object : Handler(handlerThread.looper) {
        val running = AtomicBoolean(false)

        override fun handleMessage(msg: Message) {
            when(msg.what) {
                MSG_START -> if (running.compareAndSet(false, true)) {
                    while (running.get()) {
                        val foods = MerchantManager.INSTANCE.shelf.shelf
                        var food: Food? = null
                        synchronized(foods) {
                            for (row in 0 until MerchantManager.ROW) {
                                if (food != null) break
                                for (col in 0 until MerchantManager.COL) {
                                    if (foods[row][col] != null) {
                                        food = foods[row][col]
                                        MerchantManager.INSTANCE.shelf.removeFood(row, col, food!!)
                                        break
                                    }
                                }
                            }
                        }
                        if (food != null) {
                            Thread.sleep(Random.nextLong(10000, 20000))
                        }
                    }
                }
            }
        }
    }

    private val shelfObserver = object : MerchantManager.ShelfObserver() {
        override fun onFoodAdded(row: Int, col: Int, food: Food) {
            if (!delivery.running.get()) {
                start()
            }
        }

        override fun onFoodRemoved(row: Int, col: Int, food: Food) {
            if (MerchantManager.INSTANCE.shelf.isEmpty()) {
                delivery.running.compareAndSet(true, false)
            }
        }
    }

    fun start() {
        delivery.sendMessage(Message.obtain().apply { what = MSG_START })
    }
}