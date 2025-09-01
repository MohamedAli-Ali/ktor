package com.froute.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration



fun getCurrentTime() = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()

fun getCurrentTimePlusDuration(plus: Duration) = Clock.System.now().plus(plus).toLocalDateTime(TimeZone.UTC).toString()

fun newFunction() = "Hi this is new function"
