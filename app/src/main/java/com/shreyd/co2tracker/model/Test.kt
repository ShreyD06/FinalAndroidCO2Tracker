package com.shreyd.co2tracker.model


import com.google.gson.annotations.SerializedName

data class Test(
    @SerializedName("distance")
    val distance: Int,
    @SerializedName("emission")
    val emission: Int,
    @SerializedName("endLoc")
    val endLoc: List<Double>,
    @SerializedName("endTime")
    val endTime: Long,
    @SerializedName("startLoc")
    val startLoc: List<Double>,
    @SerializedName("startTime")
    val startTime: Long,
    @SerializedName("waypoints")
    val waypoints: List<List<Double>>
)