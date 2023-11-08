package com.shreyd.co2tracker.model


import com.google.gson.annotations.SerializedName

data class JustDistance(
    @SerializedName("routes")
    val routes: List<Route>
) {
    data class Route(
        @SerializedName("localizedValues")
        val localizedValues: LocalizedValues
    ) {
        data class LocalizedValues(
            @SerializedName("distance")
            val distance: Distance,
            @SerializedName("duration")
            val duration: Duration,
            @SerializedName("staticDuration")
            val staticDuration: StaticDuration
        ) {
            data class Distance(
                @SerializedName("text")
                val text: String
            )

            data class Duration(
                @SerializedName("text")
                val text: String
            )

            data class StaticDuration(
                @SerializedName("text")
                val text: String
            )
        }
    }
}