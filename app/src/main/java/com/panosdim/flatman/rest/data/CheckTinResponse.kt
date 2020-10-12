package com.panosdim.flatman.rest.data

data class CheckTinResponse(
    val countryCode: String,
    val tinNumber: String,
    val requestDate: String,
    val validStructure: Boolean,
    val validSyntax: Boolean
)