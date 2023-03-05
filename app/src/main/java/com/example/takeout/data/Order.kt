package com.example.takeout.data

class Order(
            val userId: Long,
            val orderId: Long,
            var orderState: Int = PLACE_ORDER,
            val createTime: Long) {
    companion object {
        const val PLACE_ORDER = 0 // 用户已下单、等待制作
        const val COOKING = 1 // 商家制作中
        const val WAIT_DELIVERY = 2 // 等待骑手
    }
}

