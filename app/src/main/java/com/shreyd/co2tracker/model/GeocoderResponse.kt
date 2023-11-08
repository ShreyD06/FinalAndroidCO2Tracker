package com.shreyd.co2tracker.model


import com.google.gson.annotations.SerializedName

data class GeocoderResponse(
    @SerializedName("plus_code")
    val plusCode: PlusCode,
    @SerializedName("results")
    val results: List<Any>,
    @SerializedName("status")
    val status: String
) {
    data class PlusCode(
        @SerializedName("compound_code")
        val compoundCode: String,
        @SerializedName("global_code")
        val globalCode: String
    )
}