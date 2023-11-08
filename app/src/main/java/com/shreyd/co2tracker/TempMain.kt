package com.shreyd.co2tracker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.Menu
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import com.google.android.gms.location.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.shreyd.co2tracker.databinding.ActivityTempMainBinding
import com.shreyd.co2tracker.datastore.UserDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.io.IOException
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList
import kotlin.properties.Delegates
import kotlin.math.*
import com.shreyd.co2tracker.model.CO2Reponse
import com.shreyd.co2tracker.model.JustDistance

class TempMain : AppCompatActivity(), EasyPermissions.PermissionCallbacks  {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityTempMainBinding

    lateinit var client: ActivityRecognitionClient
    lateinit var storage: SharedPreferences
    private lateinit var userEmail: String
    private lateinit var dbUsers: DatabaseReference
    private var emissions by Delegates.notNull<Double>()
    private var lenDrives by Delegates.notNull<Int>()
    private lateinit var newDrives: MutableList<Drive>

    private val userDataStore by lazy { UserDataStore.getInstance() }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTempMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarTempMain.toolbar)

        binding.appBarTempMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_temp_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_settings, R.id.nav_ptransport, R.id.nav_coffsets
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        //TODO("Clear dataStore here")
        CoroutineScope(Dispatchers.Main).launch { userDataStore.clearLocationData() }

        val authUser = Firebase.auth.currentUser
        var email = ""
        authUser?.let{
            email = it.email!!
        }
        val id = email.replace(".", "").replace("#", "")
            .replace("$", "").replace("[", "").replace("]", "")
        userEmail = id

        val rawDrives = mutableListOf<Drive>()
        val dbRawDrives = FirebaseDatabase.getInstance().getReference("RawDrives")
        val synthDrives = mutableListOf<Drive>()
        dbUsers = FirebaseDatabase.getInstance().getReference("Users").child(userEmail)
        lenDrives = 0
        emissions = 0.0
        newDrives = mutableListOf()


        CoroutineScope(Dispatchers.IO).launch {
            val driveListener = object : ValueEventListener {
                var change = 0
                var times = 0
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    times++
                    if(times <= 1) {
                        println("NEW LOAD")
                        for (ds in dataSnapshot.children) {
                            println(change)
                            change++
                            val rdrive = Drive(
                                ds.key,
                                listOf(
                                    ds.child("startLoc").child("0").value.toString().toDouble(),
                                    ds.child("startLoc").child("1").value.toString().toDouble()
                                ),
                                listOf(
                                    ds.child("endLoc").child("0").value.toString().toDouble(),
                                    ds.child("endLoc").child("1").value.toString().toDouble()
                                ),
                                ds.child("startTime").value.toString().toLong(),
                                ds.child("endTime").value.toString().toLong()
                            )
                            rawDrives.add(rdrive)
                        }
                        for(i in 0..rawDrives.size - 2) {
                            if(rawDrives[i+1].startTime!! - rawDrives[i].endTime!! < 300000) {
                                //Synthesize drives
                                synthDrives.add(rawDrives[i])
                                if(i == rawDrives.size - 2) {
                                    val waypoints = mutableListOf<List<Double?>>()

                                    if(synthDrives.size == 1) {
                                        waypoints.add(synthDrives[0].endLoc)
                                    }
                                    else {
                                        for(k in 1..synthDrives.size - 1) {
                                            waypoints.add(synthDrives[k].startLoc)
                                        }
                                        waypoints.add(rawDrives[i].startLoc)
                                    }


                                    val newDrive = Drive(
                                        synthDrives[0].id,
                                        synthDrives[0].startLoc,
                                        rawDrives[i + 1].endLoc,
                                        synthDrives[0].startTime,
                                        rawDrives[i + 1].endTime,
                                        waypoints
                                    )

                                    dbUsers.child("Drives").child(newDrive.id!!).setValue(newDrive)
                                    newDrives.add(newDrive)
                                }
                            }
                            else if(synthDrives.size > 0){

                                val waypoints = mutableListOf<List<Double?>>()

                                if(synthDrives.size == 1) {
                                    waypoints.add(synthDrives[0].endLoc)
                                }
                                else {
                                    for(k in 1..synthDrives.size - 1) {
                                        waypoints.add(synthDrives[k].startLoc)
                                    }
                                    waypoints.add(rawDrives[i].startLoc)
                                }


                                val newDrive = Drive(
                                    synthDrives[0].id,
                                    synthDrives[0].startLoc,
                                    rawDrives[i].endLoc,
                                    synthDrives[0].startTime,
                                    rawDrives[i].endTime,
                                    waypoints
                                )

                                dbUsers.child("Drives").child(newDrive.id!!).setValue(newDrive)
                                newDrives.add(newDrive)

                                if(i == rawDrives.size - 2) {
                                    dbUsers.child(rawDrives[i+1].id!!).setValue(rawDrives[i+1])
                                    newDrives.add(rawDrives[i+1])
                                }


                                waypoints.clear()
                                synthDrives.clear()
                            }
                            else {
                                val newDrive = Drive(
                                    rawDrives[i].id,
                                    rawDrives[i].startLoc,
                                    rawDrives[i].endLoc,
                                    rawDrives[i].startTime,
                                    rawDrives[i].endTime
                                )

                                dbUsers.child("Drives").child(newDrive.id!!).setValue(newDrive)
                                newDrives.add(newDrive)

                                if(i == rawDrives.size - 2) {
                                    dbUsers.child("Drives").child(rawDrives[i+1].id!!).setValue(rawDrives[i+1])
                                    newDrives.add(rawDrives[i+1])
                                }
                            }
                        }
                    }

                }
                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message
                    println("Cancelled")
                }
            }

            dbRawDrives.addListenerForSingleValueEvent(driveListener)


            //Clear dbRawDrives here

            //Google maps API call here
            delay(5000)

            var count = 0
            val userListener = object : ValueEventListener {
                override fun onDataChange(ds: DataSnapshot) {
                    val emailId = ds.child("email").value.toString()
                    //ONLY LOAD IN DRIVES FROM HERE, MAKE API CALLS OUTSIDE onDataChange()
                    count++
                    if(count == 1) {
                        //Calling the routes api only once due to pricing, otherwise it would be called with every drive
                        val threshold2 = ds.child("Drives").childrenCount - 15


                        try {
                            for(drive in ds.child("Drives").children) {
                                if(drive.child("emission").value.toString().toDouble() == 0.0) {
                                    lenDrives++
                                }
                            }
                            var count2 = 0

                            val threshold = ds.child("Drives").childrenCount - 15

                            for(drive in ds.child("Drives").children) {
                                var tDrive = drive.getValue(Drive2::class.java)

                                if(drive.child("emission").value.toString().toDouble() == 0.0) {
                                    count2++

                                    if(ds.child("cartype").value.toString() == "Sedan") {
                                        println("MAKING API CALL")
                                        val activity_id = "passenger_vehicle-vehicle_type_car-fuel_source_na-engine_size_na-vehicle_age_na-vehicle_weight_na"
                                        carApiRequest("https://beta4.api.climatiq.io/estimate", activity_id, ds.child("Emissions").value.toString().toDouble(), drive.key, count2, threshold)
                                    }
                                    else if(ds.child("cartype").value.toString() == "Motorcycle") {
                                        println("MAKING API CALL")
                                        val activity_id = "passenger_vehicle-vehicle_type_motorcycle-fuel_source_na-engine_size_na-vehicle_age_na-vehicle_weight_na"
                                        carApiRequest("https://beta4.api.climatiq.io/estimate", activity_id, ds.child("Emissions").value.toString().toDouble(), drive.key, count2, threshold)
                                    }
                                    else if(ds.child("cartype").value.toString() == "Pickup Truck/Minivan") {
                                        println("MAKING API CALL")
                                        val activity_id = "commercial_vehicle-vehicle_type_truck_light-fuel_source_na-engine_size_na-vehicle_age_na-vehicle_weight_na"
                                        carApiRequest("https://beta4.api.climatiq.io/estimate", activity_id, ds.child("Emissions").value.toString().toDouble(), drive.key, count2, threshold)
                                    }
                                    else if(ds.child("cartype").value.toString() == "Truck") {
                                        println("MAKING API CALL")
                                        val activity_id = "passenger_vehicle-vehicle_type_motorcycle-fuel_source_na-engine_size_na-vehicle_age_na-vehicle_weight_na"
                                        carApiRequest("https://beta4.api.climatiq.io/estimate", activity_id, ds.child("Emissions").value.toString().toDouble(), drive.key, count2, threshold)
                                    }




                                }
                            }
                        }
                        catch (err: Error) {
                            println("Failed")
                        }

                        val gDrive = ds.child("Drives").child("routesTest").getValue(Drive2::class.java)
                        var partJson = ""
                        val gson = Gson()

                        var wp = 0
                        if (gDrive != null) {
                            for (waypoint in gDrive.waypoints!!) {
                                wp++
                                if(wp != gDrive.waypoints!!.size) {
                                    partJson += "{\"location\":{\"latLng\":{\"latitude\":${waypoint[0]},\"longitude\":${waypoint[1]}}},\"via\":true},"
                                }
                                else {
                                    partJson += "{\"location\":{\"latLng\":{\"latitude\":${waypoint[0]},\"longitude\":${waypoint[1]}}},\"via\":true}"
                                }
                            }
                        }


                        var okHttpClient: OkHttpClient = OkHttpClient()
                        var result: String? = null
                        val sUrl2 = "https://routes.googleapis.com/directions/v2:computeRoutes"
                        val json = "{\"origin\":{\"address\":\"1944 Horse Shoe Drive, Vienna, VA\"},\"destination\":{\"address\":\"7731 Leesburg Pike, Falls Church, VA\"},\"intermediates\":[$partJson],\"travelMode\":\"DRIVE\"}"

                        println(partJson)
                        println("----------------------")
                        println(json)
                        val body: RequestBody = json.toRequestBody("/application/json".toMediaTypeOrNull())
                        val url2 = URL(sUrl2)

                        val request2 = Request.Builder().post(body).url(url2).addHeader("Content-Type", "application/json").addHeader("X-Goog-Api-Key", Constants.GMAPKEY).addHeader("X-Goog-FieldMask", "routes.localizedValues").build()

                        okHttpClient.newCall(request2).enqueue(object: Callback {
                            override fun onResponse(call: Call, response: Response) {
                                result = response.body?.string()

                                println(result)

                                val distResp = gson.fromJson(result, JustDistance::class.java)

                                //Loop through drives and add distance to them here

                                for (drive in ds.child("Drives").children) {

                                    dbUsers.child("Drives").child(drive.key!!).child("distance").setValue(distResp.routes[0].localizedValues.distance.text)


                                }

                            }

                            override fun onFailure(call: Call, e: IOException) {
                                e.printStackTrace()
                            }

                        })

                    }

                }

                override fun onCancelled(error: DatabaseError) {
                    println("Cancelled")
                }

            }

            dbUsers.addListenerForSingleValueEvent(userListener)
//            println(newDrives)

            var iter = 0
            val renewed = mutableListOf<Drive>()
            val freqDrives = mutableMapOf<String, Int>()
            val sTimes = mutableMapOf<String, MutableList<kotlin.Long?>>()

            //Frequent Drives Calculation Code - Was used in app but commented for demo due to large amount of drives it outputted
//
//            val fDrivesListener = object : ValueEventListener {
//                override fun onDataChange(dataSnapshot: DataSnapshot) {
//                    println("READING")
//                    iter++
//                    if(iter < 2 || newDrives.size != renewed.size) {
//                        if (dataSnapshot.childrenCount == 0L) {
//                            println("Does not exist")
//                            val newFDrive = FreqDrive(dbUsers.child("Frequent Drives").push().key, newDrives[0].startLoc, newDrives[0].endLoc, mutableListOf(newDrives[0].startTime), newDrives[0].endTime, newDrives[0].waypoints, newDrives[0].emission, newDrives[0].distance)
//                            renewed.addAll(newDrives.subList(1, newDrives.size))
//                            dbUsers.child("Frequent Drives").child(newFDrive.id!!).setValue(newFDrive)
//                        }
//                        else {
//                            if (renewed.size == 0) {
//                                renewed.addAll(newDrives)
//                            }
//                            for (ds in dataSnapshot.children) {
//                                freqDrives.put(ds.key!!, ds.child("times").value.toString().toInt())
//                                sTimes.put(ds.key!!, ds.getValue(FreqDrive::class.java)!!.startTimes)
//                            }
//                            for (ds in dataSnapshot.children) {
//                                println("Setting")
//                                for (drive in renewed) {
//                                    if (calcDistance(drive.startLoc, ds.getValue(FreqDrive::class.java)?.startLoc!!) <= 100.0 &&
//                                        calcDistance(drive.endLoc, ds.getValue(FreqDrive::class.java)?.endLoc!!) <= 100.0)
//                                    {
//                                        freqDrives[ds.key!!] = freqDrives[ds.key!!]!! + 1
//                                        sTimes[ds.key!!]!!.add(drive.startTime)
//
//                                    }
//
//                                    else {
//                                        //add into Frequent Drives (with a new model class with just the locations and a time parameter)
//                                        val newFDrive = FreqDrive(dbUsers.child("Frequent Drives").push().key, drive.startLoc, drive.endLoc, mutableListOf(drive.startTime), drive.endTime, drive.waypoints, drive.emission, drive.distance)
//                                        dbUsers.child("Frequent Drives").child(newFDrive.id!!).setValue(newFDrive)
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//
//                override fun onCancelled(databaseError: DatabaseError) {
//                    // Getting Post failed, log a message
//                    println(databaseError)
//                }
//            }
//            dbUsers.child("Frequent Drives").addValueEventListener(fDrivesListener)
//
//            delay(3000)
//
//            println("Read Map")
//            freqDrives.forEach {
//                println("${it.key}, ${it.value}")
//                dbUsers.child("Frequent Drives").child(it.key).child("times").setValue(it.value)
//            }
//            sTimes.forEach {
//                println("READING THIS")
//                println("${it.key}, ${it.value}")
//                dbUsers.child("Frequent Drives").child(it.key).child("startTimes").setValue(it.value)
//            }
        }

//        CoroutineScope(Dispatchers.IO).launch {
//            val timeListener = object: ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    if (snapshot.childrenCount > 0L) {
//                        for (ds in snapshot.children) {
//                            val convTimes = mutableListOf<String>()
//                            val drive = ds.getValue(FreqDrive::class.java)
//                            if(drive!!.times >= 5) {
//                                //Find minimum time by converting times to date using SimpleDateFormat
//                                val formatter = SimpleDateFormat("hh:mm:ss.SSS", Locale.US)
//                                for (t in drive.startTimes) {
//                                    convTimes.add(formatter.format(t))
//                                    println(formatter.format(t))
//                                }
//                                val milliTimes = convTimes.map {
//                                    formatter.parse(it).time
//                                }
//                                //Find out "compactness" of times data: Find how large the standard deviation is?
//
//                                println("MIN: ${milliTimes.min()}, ${formatter.format(milliTimes.min())}")
//                                dbUsers.child("Frequent Drives").child(ds.key!!).child("finTime").setValue(milliTimes.min())
//
//                            }
//                        }
//                    }
//
//                }
//
//                override fun onCancelled(error: DatabaseError) {
//                    TODO("Not yet implemented")
//                }
//
//            }
//            dbUsers.child("Frequent Drives").addListenerForSingleValueEvent(timeListener)
//        }

        val intent = Intent(this, ActivityTransitionReceiver::class.java)
        val intent2 = Intent(this, ActivityTransitionReceiver::class.java)
        intent.action = "MYLISTENINGACTION"
        intent2.action = "MYLISTENINGACTION"
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

        val result = ActivityTransitionResult(events)
        SafeParcelableSerializer.serializeToIntentExtra(
            result, intent,
            "com.google.android.location.internal.EXTRA_ACTIVITY_TRANSITION_RESULT"
        )
        val events2: MutableList<ActivityTransitionEvent> = ArrayList()
        transitionEvent = ActivityTransitionEvent(
            DetectedActivity.IN_VEHICLE,
            ActivityTransition.ACTIVITY_TRANSITION_EXIT, SystemClock.elapsedRealtimeNanos()
        )
        events2.add(transitionEvent)

        val result2 = ActivityTransitionResult(events2)
        SafeParcelableSerializer.serializeToIntentExtra(
            result2, intent2,
            "com.google.android.location.internal.EXTRA_ACTIVITY_TRANSITION_RESULT"
        )


        CoroutineScope(Dispatchers.Main).launch {
            delay(5000)
            sendBroadcast(intent)
            delay(5000)
            sendBroadcast(intent2)
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
            println("REQUESTING PERMISSION")
            requestActivityTransitionPermission()
        } else {
            // when permission is already allowed
            println("REQUESTING UPDATES")
            requestForUpdates()
        }
//        userDataStoreDemo()
    }

//    private fun userDataStoreDemo() {
//        lifecycleScope.launch {
//            userDataStore.setAuthToken("my tiken is 0")
//
//            userDataStore.getAuthToken()?.let { Log.e("MyToken: ", it) }
//        }
//
//    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.temp_main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_temp_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }




    private fun carApiRequest(sUrl: String, activityId: String, currentEmissions: Double, drive: String?, count: Int, threshold: kotlin.Long): String? {

        var okHttpClient: OkHttpClient = OkHttpClient()
        val gson = Gson()
        var result: String? = null
        val distance = 50
        //API call commented due to monetary limit on API calls
//        if(count.toLong() > threshold) {
//            val json = "{\"emission_factor\":{\"activity_id\":\"$activityId\", \"data_version\":\"4.4\", \"region\":\"US\"}, \"parameters\":{\"distance\":$distance, \"distance_unit\":\"mi\"}}"
//            try {
//                val body: RequestBody = json.toRequestBody("/application/json".toMediaTypeOrNull())
//
//                val url = URL(sUrl)
//
//                val request = Request.Builder().post(body).url(url).addHeader("Authorization", "Bearer: ${Constants.KEY}").build()
//
//                okHttpClient.newCall(request).enqueue(object: Callback {
//                    override fun onResponse(call: Call, response: Response) {
//                        result = response.body?.string()
//
//                        val cResp = gson.fromJson(result, CO2Reponse::class.java)
//                        val em = cResp.co2e
//                        emissions + cResp.co2e
//                        newDrives[count-1].emission = em
//
//                        dbUsers.child("Drives").child(drive!!).child("emission").setValue(em)
//                    }
//
//                    override fun onFailure(call: Call, e: IOException) {
//                        e.printStackTrace()
//                    }
//                })
//
//                //calculate rest of emissions here: 16*number of remaining drives
//                return result
//            }
//            catch(err:Error) {
//                print("Error when executing get request: "+err.localizedMessage)
//            }
//        }

        emissions += 16
        dbUsers.child("Drives").child(drive!!).child("emission").setValue(16)
//        newDrives[count-1].emission = 16.0


        println(count)

        //Don't call actual API till RawDrives are empty, otherwise, the Drives node under the user will always have all drives "emission" be 0, since
        // the drives are being created new every time
        println("PSUEDO API CALL MADE")
        if(count == lenDrives) {
            dbUsers.child("Emissions").setValue(emissions)
        }
        return result
    }


    private fun calcDistance(startLoc: List<Double?>, endLoc: List<Double?>): Double {
        val radPerDeg = PI/180
        val rkm = 6371
        val rm = rkm * 100

        val dLatRad = (endLoc[0]!! - startLoc[0]!!) * radPerDeg
        val dLonRad = (endLoc[1]!! - startLoc[1]!!) * radPerDeg

        val startLatRad = startLoc[0]!! * radPerDeg
        val startLonRad = startLoc[1]!! * radPerDeg

        val endLatRad = endLoc[0]!! * radPerDeg
        val endLonRad = endLoc[1]!! * radPerDeg

        val a = sin(dLatRad/2).pow(2) + cos(startLatRad) * cos(endLatRad) * sin(dLonRad/2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1-a))

        println(rm * c)

        return rm * c
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
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)||locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER)
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
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
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
}