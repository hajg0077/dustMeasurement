package com.example.dustmeasurement.data.models.tmcoordinates


import com.google.gson.annotations.SerializedName

data class TmCoordinatesRespnse(
    @SerializedName("documents")
    val documents: List<Document>?,
    @SerializedName("meta")
    val meta: Meta?
)