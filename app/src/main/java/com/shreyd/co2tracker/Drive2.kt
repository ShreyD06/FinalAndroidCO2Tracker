package com.shreyd.co2tracker

import com.google.firebase.database.Exclude


class Drive2 {

    var id: String? = ""
    var startLoc: List<Double?> = listOf(0.0, 0.0)
    var endLoc: List<Double?> = listOf(0.0, 0.0)
    var startTime: Long? = 0
    var endTime: Long? = 0
    var waypoints: List<List<Double?>>? = null
    var emission: Double = 0.0
    var distance: String = ""
    var savedEms: Double = 0.0

    private constructor() {}
    constructor(id: String?,
                startLoc: List<Double?>,
                endLoc: List<Double?>,
                startTime: Long?,
                endTime: Long?,
                waypoints: List<List<Double?>>?,
                emission: Double,
                distance: String,
                savedEms: Double
                ) {
        this.id = id
        this.startLoc = startLoc
        this.endLoc  = endLoc
        this.startTime = startTime
        this.endTime = endTime
        this.waypoints = waypoints
        this.emission = emission
        this.distance = distance
        this.savedEms = savedEms
    }
}

