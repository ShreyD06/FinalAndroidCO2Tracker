package com.shreyd.co2tracker

import com.google.firebase.database.Exclude

class FreqDrive {
    var id: String? = ""
    var startLoc: List<Double?> = listOf(0.0, 0.0)
    var endLoc: List<Double?> = listOf(0.0, 0.0)
    var startTimes: MutableList<Long?> = mutableListOf()
    var endTime: Long? = 0
    var waypoints: List<List<Double?>>? = null
    var emission: Double = 0.0
    var distance: Double = 0.0
    var times: Int = 0
    var finTime: Long? = 0
    var startLocS: String = "1944 Horse Shoe Dr"
    var endLocS: String = "7731 Leesburg Pike"

    private constructor() {}
    constructor(id: String?,
                startLoc: List<Double?>,
                endLoc: List<Double?>,
                startTimes: MutableList<Long?>,
                endTime: Long?,
                waypoints: List<List<Double?>>?,
                emission: Double,
                distance: Double
                ) {
        this.id = id
        this.startLoc = startLoc
        this.endLoc  = endLoc
        this.startTimes = startTimes
        this.endTime = endTime
        this.waypoints = waypoints
        this.emission = emission
        this.distance = distance
    }
}