package com.example.takeout.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.takeout.R
import com.example.takeout.data.Food
import com.example.takeout.delivery.Delivery
import com.example.takeout.merchant.MerchantManager
import com.example.takeout.user.UserPlaceOrder
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    init {
        MerchantManager.INSTANCE.init()
        UserPlaceOrder.INSTANCE.start()
        Delivery.INSTANCE.start()
    }

    private lateinit var shelfObserver: MerchantManager.ShelfObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val gridLayoutManager = GridLayoutManager(this, MerchantManager.ROW)

        recyclerview.layoutManager = gridLayoutManager

        val adapter = object : Adapter<FoodViewHolder>() {

            val data = MerchantManager.INSTANCE.shelf.shelf

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
                return FoodViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_shelf, parent, false))
            }

            override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
                if (data[position / MerchantManager.ROW][position % MerchantManager.ROW] == null) {
                    holder.itemView.visibility = View.INVISIBLE
                } else {
                    holder.itemView.visibility = View.VISIBLE
                }
            }

            override fun getItemCount(): Int = MerchantManager.ROW * MerchantManager.COL
        }

        recyclerview.adapter = adapter

        shelfObserver = object : MerchantManager.ShelfObserver() {
            override fun onFoodAdded(row: Int, col: Int, food: Food) {
                recyclerview.post { adapter.notifyItemChanged(row * MerchantManager.ROW + col) }
            }

            override fun onFoodRemoved(row: Int, col: Int, food: Food) {
                recyclerview.post { adapter.notifyItemChanged(row * MerchantManager.ROW + col) }
            }
        }

        MerchantManager.INSTANCE.shelf.subscribe(shelfObserver)
    }

    override fun onDestroy() {
        MerchantManager.INSTANCE.shelf.unSubscribe(shelfObserver)
        super.onDestroy()
    }

    class FoodViewHolder(itemView: View) : ViewHolder(itemView)
}