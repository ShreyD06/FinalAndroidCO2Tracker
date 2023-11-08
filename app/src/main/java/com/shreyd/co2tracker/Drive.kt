package com.shreyd.co2tracker

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.firebase.database.Exclude
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Drive(
    @get:Exclude
    var id: String?,
    var startLoc: List<Double?> = listOf(0.0, 0.0),
    var endLoc: List<Double?> = listOf(0.0, 0.0),
    var startTime: Long? = 0,
    var endTime: Long? = 0,
    var waypoints: List<List<Double?>>? = null,
    var emission: Double = 0.0,
    var distance: String = "",
    var savedEms: Double = 0.0
    )
{
}

