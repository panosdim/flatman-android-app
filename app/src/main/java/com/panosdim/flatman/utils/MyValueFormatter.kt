package com.panosdim.flatman.utils

import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

class MyValueFormatter : ValueFormatter() {
    private val symbols = DecimalFormatSymbols()

    init {
        symbols.groupingSeparator = '.'
        symbols.decimalSeparator = ','
    }

    private val format = DecimalFormat("#,##0 €", symbols)

    override fun getPointLabel(entry: Entry?): String {
        return format.format(entry?.y)
    }
}