package com.shreyd.co2tracker

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import com.google.android.gms.location.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.shreyd.co2tracker.Constants.ACTIVITY_TRANSITION_STORAGE
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import com.shreyd.co2tracker.ActivityTransitionReceiver
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


// Create a button that starts the foreground service for activity transition recognition, then add the restart on reboot functionality


//Unused Class
class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    lateinit var client: ActivityRecognitionClient
    lateinit var storage: SharedPreferences


    lateinit var button: Button


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rawDrives = mutableListOf<Drive>()
        val dbRawDrives = FirebaseDatabase.getInstance().getReference("RawDrives")
        val synthDrives = mutableListOf<Drive>()

        val driveListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (ds in dataSnapshot.children) {
                    val rdrive = Drive(ds.key,
                        listOf(ds.child("startLoc").child("0").value.toString().toDouble(), ds.child("startLoc").child("1").value.toString().toDouble()),
                        listOf(ds.child("endLoc").child("0").value.toString().toDouble(), ds.child("endLoc").child("1").value.toString().toDouble()),
                        ds.child("startTime").value.toString().toLong(),
                        ds.child("endTime").value.toString().toLong())
                    rawDrives.add(rdrive)
                }
                for(i in 0..rawDrives.size - 2) {
                    if(rawDrives[i+1].startTime!! - rawDrives[i].endTime!! < 300000) {
                        //Synthesize drives
                        synthDrives.add(rawDrives[i])
                    }
                    else {
                        val waypoints = mutableListOf<List<Double?>>()
                        for(k in 1..synthDrives.size - 2) {
                            waypoints.add(synthDrives[k].endLoc)
                        }
                        val newDrive = Drive("",
                            synthDrives[0].startLoc,
                            synthDrives[synthDrives.size - 1].endLoc,
                            synthDrives[0].startTime,
                            synthDrives[synthDrives.size - 1].endTime,
                            waypoints
                            )
                        val dbDrives = FirebaseDatabase.getInstance().getReference("Drives")
                        newDrive.id = dbDrives.push().key
                        dbDrives.child(newDrive.id!!).setValue(newDrive)
                        waypoints.clear()
                        synthDrives.clear()
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                println("Cancelled")
            }
        }

        dbRawDrives.addValueEventListener(driveListener)
        //Clear dbRawDrives here

        button = findViewById(R.id.btnOpenAct2)
        button.setOnClickListener {
            val intent = Intent(this, Activity2::class.java)
            startActivity(intent)
        }


        // The Activity Recognition Client returns a
        // list of activities that a user might be doing
        client = ActivityRecognition.getClient(this)

        // variable to check whether the user have already given the permissions
        storage = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)

                // check for devices with Android 10 (29+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    // check for permission
            && !ActivityTransitionsUtil.hasActivityTransitionPermissions(this)
        ) {
                    // request for permission
            requestActivityTransitionPermission()
        } else {
                    // when permission is already allowed
            requestForUpdates()
        }

        println("Creating Intent")
        val intent = Intent(this, ActivityTransitionReceiver::class.java)
        intent.action = "MYLISTENINGACTION"
        val events: MutableList<ActivityTransitionEvent> = ArrayList()
        var transitionEvent: ActivityTransitionEvent
        transitionEvent = ActivityTransitionEvent(
            DetectedActivity.STILL,
            ActivityTransition.ACTIVITY_TRANSITION_EXIT, SystemClock.elapsedRealtimeNanos()
        )
        events.add(transitionEvent)
        transitionEvent = ActivityTransitionEvent(
            DetectedActivity.IN_VEHICLE,
            ActivityTransition.ACTIVITY_TRANSITION_ENTER, SystemClock.elapsedRealtimeNanos()
        )
        events.add(transitionEvent)
        transitionEvent = ActivityTransitionEvent(
            DetectedActivity.IN_VEHICLE,
            ActivityTransition.ACTIVITY_TRANSITION_EXIT, SystemClock.elapsedRealtimeNanos()
        )
        events.add(transitionEvent)
        val result = ActivityTransitionResult(events)
        SafeParcelableSerializer.serializeToIntentExtra(
            result, intent,
            "com.google.android.location.internal.EXTRA_ACTIVITY_TRANSITION_RESULT"
        )
        sendBroadcast(intent)
        println("Sent Broadcast")


    }


    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Subscribe
    fun onDataReceived(event: ActivityTransitionReceiver.OnReceiverEvent) {
        // These 2 lines are for debug purposes
        print(event.getEvents())
        print("----------GOING TO SERVICE-------------")

        val serviceIntentEnter: Intent = Intent(this, MyService::class.java).apply {
            action=MyService.ENTER
        }
        val serviceIntentExit: Intent = Intent(this, MyService::class.java).apply {
            action=MyService.EXIT
        }

        fun isLocationEnabled(): Boolean {
            val locationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)||locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }

        fun requestPermission() {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        }

        fun checkPermissions(): Boolean {
            if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) {
                return true
            }
            return false
        }
        if(checkPermissions()) {
            if(isLocationEnabled()) {
                    //latitude and longitude shown here
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    println("Second check passed")
                }
                if(event.getEvents()[1] == "ENTER") {
                    startForegroundService(serviceIntentEnter)
                }

                else {
                    startForegroundService(serviceIntentExit)
                }
            }
            else {
                    //settings open here
                Toast.makeText(this, "Turn on location", Toast.LENGTH_SHORT).show()
                val intent=Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }
        else {
                //request permission
            requestPermission()
        }
    }

    // when permission is denied
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        // permission is denied permanently
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestActivityTransitionPermission()
        }
    }

    // after giving permission
    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        requestForUpdates()
    }

    // request for permission
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)


    }

    // To register for changes we have to also supply the requestActivityTransitionUpdates() method
    // with the PendingIntent object that will contain an intent to the component
    // (i.e. IntentService, BroadcastReceiver etc.) that will receive and handle updates appropriately.
    private fun requestForUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            println("Not registered, reboot app")
        }
        else {
            client
                .requestActivityTransitionUpdates(
                    ActivityTransitionsUtil.getActivityTransitionRequest(),
                    getPendingIntent()
                )
                .addOnSuccessListener {
                    showToast("successful registration")
                    println("REGISTERED")
                }
                .addOnFailureListener {
                    showToast("Unsuccessful registration")
                    println("NOT REGISTERED")
                }

        }

    }

    // Deregistering from updates
    // call the removeActivityTransitionUpdates() method
    // of the ActivityRecognitionClient and pass
    // ourPendingIntent object as a parameter
    private fun deregisterForUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            println("Not registered, reboot app")
        }
        else {
            client
                .removeActivityTransitionUpdates(getPendingIntent())
                .addOnSuccessListener {
                    getPendingIntent().cancel()
                    showToast("successful deregistration")
                }
                .addOnFailureListener { e: Exception ->
                    showToast("unsuccessful deregistration")
                }
        }
    }

    // creates and returns the PendingIntent object which holds
    // an Intent to an BroadCastReceiver class
    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(this, ActivityTransitionReceiver::class.java)
        return PendingIntent.getBroadcast(
            this,
            Constants.REQUEST_CODE_INTENT_ACTIVITY_TRANSITION,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }



    // requesting for permission
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestActivityTransitionPermission() {
        EasyPermissions.requestPermissions(
            this,
            "You need to allow Activity Transition Permissions in order to recognize your activities",
            Constants.REQUEST_CODE_ACTIVITY_TRANSITION,
            Manifest.permission.ACTIVITY_RECOGNITION
        )
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG)
            .show()
    }

    // save switch state
    private fun saveSwitchState(value: Boolean) {
        storage
            .edit()
            .putBoolean(ACTIVITY_TRANSITION_STORAGE, value)
            .apply()
    }

    // get the state of switch
    private fun getSwitchState() = storage.getBoolean(ACTIVITY_TRANSITION_STORAGE, false)



//    private fun getCurrentLocation() {
//        if(checkPermissions()) {
//            if(isLocationEnabled()) {
//                //latitude and longitude shown here
//                if (ActivityCompat.checkSelfPermission(
//                        this,
//                        Manifest.permission.ACCESS_FINE_LOCATION
//                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
//                        this,
//                        Manifest.permission.ACCESS_COARSE_LOCATION
//                    ) != PackageManager.PERMISSION_GRANTED
//                ) {
//                    println("Second check passed")
//                }
//                startService(serviceIntent)
//            }
//            else {
//                //settings open here
//                Toast.makeText(this, "Turn on location", Toast.LENGTH_SHORT).show()
//                val intent=Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
//                startActivity(intent)
//            }
//        }
//        else {
//            //request permission
//            requestPermission()
//        }
//    }
//
//    private fun isLocationEnabled(): Boolean {
//        val locationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
//        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)||locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
//    }
//
//    private fun requestPermission() {
//        ActivityCompat.requestPermissions(
//            this, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION),
//            PERMISSION_REQUEST_ACCESS_LOCATION
//        )
//    }
//
//    companion object{
//        private const val PERMISSION_REQUEST_ACCESS_LOCATION=100
//    }
//
//    private fun checkPermissions(): Boolean {
//        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED &&
//            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) {
//            return true
//        }
//        return false
//    }

}



//import android.Manifest
//import android.content.Context
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.location.Location
//import android.location.LocationManager
//import androidx.appcompat.app.AppCompatActivity
//import android.os.Bundle
//import android.provider.Settings
//import android.widget.TextView
//import android.widget.Toast
//import androidx.core.app.ActivityCompat
//import com.google.android.gms.location.FusedLocationProviderClient
//import com.google.android.gms.location.LocationServices
//
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
//    private lateinit var tvLatitude: TextView
//    private lateinit var tvLongitude: TextView
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(this)
//        tvLatitude=findViewById(R.id.tv_Latitude)
//        tvLongitude=findViewById(R.id.tv_Longitude)
//
//        getCurrentLocation()
//    }
//

//}
