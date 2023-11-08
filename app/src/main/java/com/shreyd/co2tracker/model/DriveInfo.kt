package com.shreyd.co2tracker.model

/**
 * Created by Jyotish Biswas on 08,October,2023
 */
data class DriveInfo (

    var distance: Int? = null,
    var emission: Double? = null,
    var endTime: Long? = null,
    var startTime: Long? = null,

    var endLoc: List<Double>? = null,
    var startLoc: List<Double>? = null,
    val waypoints: List<List<Double>>? = null

)