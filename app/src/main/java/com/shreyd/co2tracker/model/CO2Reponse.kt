package com.shreyd.co2tracker.model


import com.google.gson.annotations.SerializedName

data class CO2Reponse(
    @SerializedName("activity_data")
    val activityData: ActivityData,
    @SerializedName("audit_trail")
    val auditTrail: String,
    @SerializedName("co2e")
    val co2e: Double,
    @SerializedName("co2e_calculation_method")
    val co2eCalculationMethod: String,
    @SerializedName("co2e_calculation_origin")
    val co2eCalculationOrigin: String,
    @SerializedName("co2e_unit")
    val co2eUnit: String,
    @SerializedName("constituent_gases")
    val constituentGases: ConstituentGases,
    @SerializedName("emission_factor")
    val emissionFactor: EmissionFactor
) {
    data class ActivityData(
        @SerializedName("activity_unit")
        val activityUnit: String,
        @SerializedName("activity_value")
        val activityValue: Double
    )

    data class ConstituentGases(
        @SerializedName("ch4")
        val ch4: Double,
        @SerializedName("co2")
        val co2: Double,
        @SerializedName("co2e_other")
        val co2eOther: Any,
        @SerializedName("co2e_total")
        val co2eTotal: Any,
        @SerializedName("n2o")
        val n2o: Double
    )

    data class EmissionFactor(
        @SerializedName("access_type")
        val accessType: String,
        @SerializedName("activity_id")
        val activityId: String,
        @SerializedName("category")
        val category: String,
        @SerializedName("data_quality_flags")
        val dataQualityFlags: List<Any>,
        @SerializedName("id")
        val id: String,
        @SerializedName("name")
        val name: String,
        @SerializedName("region")
        val region: String,
        @SerializedName("source")
        val source: String,
        @SerializedName("source_dataset")
        val sourceDataset: String,
        @SerializedName("source_lca_activity")
        val sourceLcaActivity: String,
        @SerializedName("year")
        val year: Int
    )
}