package com.shreyd.co2tracker

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.location.Location
import android.os.Build

import android.provider.Settings
import android.util.Log
import android.widget.Toast

import androidx.annotation.RequiresApi
import com.google.android.gms.location.*
import androidx.core.app.ActivityCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.shreyd.co2tracker.datastore.UserDataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class UserPreferences(
    val LatEnter: Double?,
    val LongEnter: Double?,
    val startTime: String?,
    val LatExit: Double?,
    val LongExit: Double?
)
class MyService: Service() {
    private lateinit var client: FusedLocationProviderClient
    val path = "/json/locations.json"
    private val Context.dataStore by preferencesDataStore(
        name = "Locations"
    )
    private val userDataStore by lazy { UserDataStore.getInstance() }
    private lateinit var scope: CoroutineScope
    override fun onCreate() {
        super.onCreate()
        client = LocationServices.getFusedLocationProviderClient(this)
        val job = SupervisorJob()
        scope = CoroutineScope(Dispatchers.IO + job)

    }
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ENTER -> enter()
            EXIT -> exit()
        }

        return super.onStartCommand(intent, flags, startId)
    }

//    private suspend fun writeStore(key: String, value: Double) {
//        val dataStoreKey = doublePreferencesKey(key)
//        dataStore.edit {settings ->
//            settings[dataStoreKey] = value
//        }
//    }
//
//    private suspend fun writeStoreLong(key: String, value: Long) {
//        val dataStoreKey = longPreferencesKey(key)
//        dataStore.edit {settings ->
//            settings[dataStoreKey] = value
//        }
//    }
//
//    private suspend fun readStore(key: String): Double? {
//        val dataStoreKey = doublePreferencesKey(key)
//        val preferences = dataStore.data.first()
//        return preferences[dataStoreKey]
//    }
//
//    private suspend fun readStoreLong(key: String): Long? {
//        val dataStoreKey = longPreferencesKey(key)
//        val preferences = dataStore.data.first()
//        return preferences[dataStoreKey]
//    }
//
//    private fun readFullStore(): Flow<UserPreferences> {
//        return dataStore.data.map { pref ->
//            UserPreferences(pref[doublePreferencesKey("LatEnter")],
//                pref[doublePreferencesKey("LongEnter")],
//                pref[stringPreferencesKey("startTime")],
//                pref[doublePreferencesKey("LatExit")],
//                pref[doublePreferencesKey("LongExit")]
//            )
//
//        }
//    }
//
//    private suspend fun deleteAllPreferences() {
//        dataStore.edit { preferences ->
//            preferences.clear()
//        }
//    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun enter() {
        startForeground(NOTIFICATION_ID, createNotification())

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            println("do nothing")
        }
        client.lastLocation.addOnSuccessListener { location: Location? ->
            val latitude: Double? = location?.latitude
            var latitude_new = 0.0

            if(latitude!=null) {
                latitude_new = latitude
            }
            val longitude: Double? = location?.longitude
            var longitude_new = 0.0
            if(longitude!=null) {
                longitude_new = longitude
            }

            println("Storing Data")
            scope.launch {
//                deleteAllPreferences()
//                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
//                val startTime = LocalDateTime.now().format(formatter)
                val startTime = System.currentTimeMillis()
                userDataStore.writeStoreLatEnter(latitude_new)
                userDataStore.writeStoreLongEnter(longitude_new)
                userDataStore.writeStoreStartTime(startTime)

            }
            println("Start: $latitude, $longitude")
        }

        stopSelf()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun exit() {
        startForeground(NOTIFICATION_ID, createNotification())


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //Log.e("startTime", startTime)
            println("do nothing")
        }
        client.lastLocation.addOnSuccessListener { location: Location? ->
            val latitude: Double? = location?.latitude
            val longitude: Double? = location?.longitude
            val startCoordinates = mutableListOf<Double?>()
            val endCoordinates = mutableListOf<Double?>()
            var latitudeExN = 0.0
            if(latitude!=null) {
                latitudeExN = latitude + 0.5
            }
            var longitudeExN = 0.0
            if(longitude!=null) {
                longitudeExN = longitude + 2.3
            }
            scope.launch {

//                readFullStore().catch{e -> e.printStackTrace()}.collect {up ->
//                    println("STARTING")
//                    startCoordinates.addAll(arrayOf(up.LatEnter, up.LongEnter))
//                    println(startCoordinates)
//                    println("Done 1")
//                    endCoordinates.addAll(arrayOf(up.LatExit, up.LongExit))
//                    println(endCoordinates)
//                    println("Done 2")
//                    startTime = up.startTime
//                    println(startTime)
//                    println("Done 3")
//                    this.cancel()
//                }
                startCoordinates.addAll(arrayOf(userDataStore.readStoreLatEnter(), userDataStore.readStoreLongEnter()))
                endCoordinates.addAll(arrayOf(latitudeExN, longitudeExN))
                val startTime = userDataStore.readStoreStartTime()
                println(startCoordinates)
                println(endCoordinates)
                println(startTime)
                println("Done reading store")

//                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val endTime = System.currentTimeMillis()

                val dbRawDrives = FirebaseDatabase.getInstance().getReference("RawDrives")
                val drive = com.shreyd.co2tracker.Drive(
                    "",
                    startCoordinates,
                    endCoordinates,
                    startTime,
                    endTime
                )
                drive.id = dbRawDrives.push().key
                dbRawDrives.child(drive.id!!).setValue(drive)


            }

            println("EXIT FUNCTION")
        }

        stopSelf()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createNotification(): Notification {
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel("location_service", "Location Service")
            } else {
                ""
            }

        val notificationBuilder = Notification.Builder(this, channelId)
            .setContentTitle("Location Service")
            .setContentText("Running")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setCategory(Notification.CATEGORY_SERVICE)

        return notificationBuilder.build()
    }

    @Suppress("SameParameterValue")
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(channel)
            channel
        } else {
            null
        }
        return channelId
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val ENTER = "ENTER"
        const val EXIT = "EXIT"
    }


}