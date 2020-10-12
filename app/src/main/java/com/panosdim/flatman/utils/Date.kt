package com.panosdim.flatman.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

fun LocalDate.toEpochMilli(): Long {
    return this.toEpochDay() * (1000 * 60 * 60 * 24)
}

fun fromEpochMilli(date: Long): LocalDate {
    return LocalDate.ofEpochDay(date / (1000 * 60 * 60 * 24))
}

fun LocalDate.toShowDateFormat(): String {
    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    return this.format(formatter)
}

fun String.toSQLDateFormat(): String {
    val parts = this.split('-')
    if (parts.size != 3) {
        return this
    }
    return parts[2] + "-" + parts[1] + "-" + parts[0]
}

fun String.toLocalDate(): LocalDate {
    return try {
        LocalDate.parse(
            this,
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
        )
    } catch (ex: DateTimeParseException) {
        LocalDate.now()
    }
}