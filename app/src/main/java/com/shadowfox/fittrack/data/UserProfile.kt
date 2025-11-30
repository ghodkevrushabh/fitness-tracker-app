package com.shadowfox.fittrack.data

data class UserProfile(
    val name: String,
    val age: String,
    val height: String,
    val startWeight: String,
    val goalWeight: String,
    val profilePicUri: String = "" // Default to empty so old code still works
)