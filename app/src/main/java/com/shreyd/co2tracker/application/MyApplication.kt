package com.shreyd.co2tracker.application

import android.app.Application
import android.content.res.Resources

/**
 * Created by Jyotish Biswas on 26,August,2023
 */
class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        res = resources
    }

    companion object {
        lateinit var instance: MyApplication
        lateinit var res: Resources
    }
}