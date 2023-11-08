package com.shreyd.co2tracker.model


import com.google.gson.annotations.SerializedName

data class RoutesResponse(
    @SerializedName("routes")
    val routes: List<Route>
) {
    data class Route(
        @SerializedName("legs")
        val legs: List<Leg>
    ) {
        data class Leg(
            @SerializedName("polyline")
            val polyline: Polyline,
            @SerializedName("steps")
            val steps: List<Step>
        ) {
            data class Polyline(
                @SerializedName("encodedPolyline")
                val encodedPolyline: String
            )

            data class Step(
                @SerializedName("navigationInstruction")
                val navigationInstruction: NavigationInstruction,
                @SerializedName("transitDetails")
                val transitDetails: TransitDetails
            ) {
                data class NavigationInstruction(
                    @SerializedName("instructions")
                    val instructions: String,
                    @SerializedName("maneuver")
                    val maneuver: String
                )

                data class TransitDetails(
                    @SerializedName("headsign")
                    val headsign: String,
                    @SerializedName("localizedValues")
                    val localizedValues: LocalizedValues,
                    @SerializedName("stopCount")
                    val stopCount: Int,
                    @SerializedName("stopDetails")
                    val stopDetails: StopDetails,
                    @SerializedName("transitLine")
                    val transitLine: TransitLine
                ) {
                    data class LocalizedValues(
                        @SerializedName("arrivalTime")
                        val arrivalTime: ArrivalTime,
                        @SerializedName("departureTime")
                        val departureTime: DepartureTime
                    ) {
                        data class ArrivalTime(
                            @SerializedName("time")
                            val time: Time,
                            @SerializedName("timeZone")
                            val timeZone: String
                        ) {
                            data class Time(
                                @SerializedName("text")
                                val text: String
                            )
                        }

                        data class DepartureTime(
                            @SerializedName("time")
                            val time: Time,
                            @SerializedName("timeZone")
                            val timeZone: String
                        ) {
                            data class Time(
                                @SerializedName("text")
                                val text: String
                            )
                        }
                    }

                    data class StopDetails(
                        @SerializedName("arrivalStop")
                        val arrivalStop: ArrivalStop,
                        @SerializedName("arrivalTime")
                        val arrivalTime: String,
                        @SerializedName("departureStop")
                        val departureStop: DepartureStop,
                        @SerializedName("departureTime")
                        val departureTime: String
                    ) {
                        data class ArrivalStop(
                            @SerializedName("location")
                            val location: Location,
                            @SerializedName("name")
                            val name: String
                        ) {
                            data class Location(
                                @SerializedName("latLng")
                                val latLng: LatLng
                            ) {
                                data class LatLng(
                                    @SerializedName("latitude")
                                    val latitude: Double,
                                    @SerializedName("longitude")
                                    val longitude: Double
                                )
                            }
                        }

                        data class DepartureStop(
                            @SerializedName("location")
                            val location: Location,
                            @SerializedName("name")
                            val name: String
                        ) {
                            data class Location(
                                @SerializedName("latLng")
                                val latLng: LatLng
                            ) {
                                data class LatLng(
                                    @SerializedName("latitude")
                                    val latitude: Double,
                                    @SerializedName("longitude")
                                    val longitude: Double
                                )
                            }
                        }
                    }

                    data class TransitLine(
                        @SerializedName("agencies")
                        val agencies: List<Agency>,
                        @SerializedName("color")
                        val color: String,
                        @SerializedName("name")
                        val name: String,
                        @SerializedName("nameShort")
                        val nameShort: String,
                        @SerializedName("textColor")
                        val textColor: String,
                        @SerializedName("vehicle")
                        val vehicle: Vehicle
                    ) {
                        data class Agency(
                            @SerializedName("name")
                            val name: String,
                            @SerializedName("phoneNumber")
                            val phoneNumber: String,
                            @SerializedName("uri")
                            val uri: String
                        )

                        data class Vehicle(
                            @SerializedName("iconUri")
                            val iconUri: String,
                            @SerializedName("name")
                            val name: Name,
                            @SerializedName("type")
                            val type: String
                        ) {
                            data class Name(
                                @SerializedName("text")
                                val text: String
                            )
                        }
                    }
                }
            }
        }
    }
}