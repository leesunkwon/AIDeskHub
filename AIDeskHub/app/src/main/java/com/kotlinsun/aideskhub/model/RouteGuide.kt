package com.kotlinsun.aideskhub.model

data class RouteGuide(
    val destinationName: String,
    val answer: String,
    val routeSteps: List<String>,
    val keywords: List<String>,
)
