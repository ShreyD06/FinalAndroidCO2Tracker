package com.shreyd.co2tracker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.maps.android.PolyUtil
import com.shreyd.co2tracker.model.GeocoderResponse
import com.shreyd.co2tracker.model.RoutesResponse
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import java.net.URL




class PublicTransport : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var routeResult: RoutesResponse
    private lateinit var polyline: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_public_transport)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapRoute) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        var button: Button = findViewById(R.id.pTransportButton)
        val authUser = Firebase.auth.currentUser
        var email = ""
        authUser?.let{
            email = it.email!!
        }
        val id = email.replace(".", "").replace("#", "")
            .replace("$", "").replace("[", "").replace("]", "")

        val dbUsers = FirebaseDatabase.getInstance().getReference("Users").child(id)
        button.setOnClickListener {

            val emmitListener = object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    println("listening")
                    println(snapshot.child("Frequent Drives").child("NfYnlMfZ2mOpk4WMPZF").child("emission").value.toString())
                    dbUsers.child("Saved Emissions").setValue(snapshot.child("Saved Emissions").value.toString().toDouble() +
                            snapshot.child("Frequent Drives").child(intent.getStringExtra("driveId")!!).child("emission").value.toString().toDouble())
                }

                override fun onCancelled(error: DatabaseError) {
                    println(error)
                }

            }


            dbUsers.addListenerForSingleValueEvent(emmitListener)
        }

        val eButton: Button = findViewById(R.id.exit)

        eButton.setOnClickListener {
            println("Clicked!")
            val tmIntent = Intent(this, TempMain::class.java)
            tmIntent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivityIfNeeded(tmIntent, 0)
        }


        val lat = "38.9081476"
        val lon = "-77.2240058"
        val sUrl = "https://maps.googleapis.com/maps/api/geocode/json?latlng=$lat,$lon&result_type=route&key=${Constants.GMAPKEY}"

//
//        val url = URL(sUrl)
//
//        val request = Request.Builder().url(url).build()
//
//        println(request.toString())
////

//        okHttpClient.newCall(request).enqueue(object: Callback {
//            override fun onResponse(call: Call, response: Response) {
//
//                result = response.body?.string()
//                println(result)
//                val geocoderResp = gson.fromJson(result, GeocoderResponse::class.java)
//
//            }
//
//            override fun onFailure(call: Call, e: IOException) {
//                e.printStackTrace()
//            }
//
//        })



    }

    override fun onMapReady(map: GoogleMap) {
        println("--------MAP READY--------")

        var okHttpClient: OkHttpClient = OkHttpClient()

        var result: String? = null
//
        val gson = Gson()
//
        val sUrl2 = "https://routes.googleapis.com/directions/v2:computeRoutes"

        val json = "{\"origin\":{\"address\":\"1944 Horse Shoe Drive, Vienna, VA\"},\"destination\":{\"address\":\"7731 Leesburg Pike, Falls Church, VA\"},\"travelMode\":\"TRANSIT\"}"

        val body: RequestBody = json.toRequestBody("/application/json".toMediaTypeOrNull())
        val url2 = URL(sUrl2)

        val request2 = Request.Builder().post(body).url(url2).addHeader("Content-Type", "application/json").addHeader("X-Goog-Api-Key", Constants.GMAPKEY).addHeader("X-Goog-FieldMask", "routes.legs.polyline,routes.legs.steps.navigationInstruction,routes.legs.steps.transitDetails").build()

        println("outside")

        CoroutineScope(Dispatchers.Main).launch {

            okHttpClient.newCall(request2).enqueue(object: Callback {
                override fun onResponse(call: Call, response: Response) {
                    result = response.body?.string()
                    routeResult = gson.fromJson(result, RoutesResponse::class.java)
                    polyline = routeResult.routes[0].legs[0].polyline.encodedPolyline
                    println(result)

                }

                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

            })


//            routeResult = gson.fromJson(Constants.response, RoutesResponse::class.java)
//            polyline = routeResult.routes[0].legs[0].polyline.encodedPolyline

            delay(4000)

            val resp = PolyUtil.decode(polyline)
            println(resp)
            val polyO = PolylineOptions()
            resp.forEach {
                polyO.add(it)
            }
            map.addPolyline(polyO)

            val markers = routeResult.routes[0].legs[0].steps
            println(markers.size)
            val markerO = MarkerOptions()
            markers.forEach {
                if(it.transitDetails != null) {
                    map.addMarker(markerO.position(LatLng(
                        it.transitDetails.stopDetails.arrivalStop.location.latLng.latitude,
                        it.transitDetails.stopDetails.arrivalStop.location.latLng.longitude)).title("Marker!"))
                }
            }

            map.moveCamera(CameraUpdateFactory.newLatLngZoom(resp[0], 14f))

            val stops: TextView = findViewById(R.id.stops)
            //Transit Type + Depart Dest + Arrival Dest
            markers.forEach {
                if(it.transitDetails != null) {
                    val someString = "${it.transitDetails.transitLine.vehicle.name.text}       ${it.transitDetails.stopDetails.departureStop.name} -> ${it.transitDetails.stopDetails.arrivalStop.name}\n${it.transitDetails.transitLine.name}\n${it.transitDetails.transitLine.nameShort}\n\n"
                    stops.text = stops.text as String + someString
                }

            }
        }

        println("Inside")



    }
    //routes.legs.steps.transitDetails
    //routes.legs.steps.polyline,routes.legs.steps.navigationInstruction,routes.legs.steps.transitDetails
}