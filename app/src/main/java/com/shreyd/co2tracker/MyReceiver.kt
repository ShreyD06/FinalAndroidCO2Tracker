package com.shreyd.co2tracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

//Unused Class
class MyReceiver: BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        println("Received")
    }
}