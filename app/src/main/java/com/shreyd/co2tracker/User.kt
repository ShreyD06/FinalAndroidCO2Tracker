package com.shreyd.co2tracker

import com.google.firebase.database.Exclude

data class User(
    @get:Exclude
    var id: String?,
    var name: String,
    var email: String,
    var password: String,
    var cartype: String
)