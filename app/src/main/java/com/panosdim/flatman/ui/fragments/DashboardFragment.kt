package com.panosdim.flatman.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.panosdim.flatman.R
import com.panosdim.flatman.balanceList
import com.panosdim.flatman.model.Balance
import com.panosdim.flatman.utils.MyValueFormatter
import com.panosdim.flatman.utils.MyXAxisFormatter
import com.panosdim.flatman.utils.moneyFormat
import kotlin.math.abs


class DashboardFragment : Fragment() {
    private lateinit var chartData: List<Entry>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_dashboard, container, false)

        val totalIncome: TextView = root.findViewById(R.id.total_income)
        val totalExpenses: TextView = root.findViewById(R.id.total_expenses)
        val totalSavings: TextView = root.findViewById(R.id.total_savings)
        val chart: LineChart = root.findViewById(R.id.chart)

        balanceList.observe(viewLifecycleOwner, { bal ->
            val balList = bal.map { it.copy() }
            val totInc = balList.map { it.amount }.reduce { tot, next ->
                if (next > 0) tot + next else tot
            }

            val totExp = balList.map { it.amount }.reduce { tot, next ->
                if (next < 0) tot + next else tot
            }
            val totSav = totInc - abs(totExp)
            totalIncome.text = moneyFormat(totInc)
            totalExpenses.text = moneyFormat(abs(totExp))
            totalSavings.text = moneyFormat(totSav)

            chartData = calculateChartData(balList)
            initializeChart(chart)
        })

        return root
    }

    private fun initializeChart(chart: LineChart) {
        val set = LineDataSet(chartData, "Savings Per Year")
        set.color = ContextCompat.getColor(
            requireContext(),
            R.color.secondaryDarkColor
        )
        set.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        set.cubicIntensity = 0.05f
        set.setCircleColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.secondaryColor
            )
        )
        set.circleHoleColor =
            ContextCompat.getColor(
                requireContext(),
                R.color.secondaryLightColor
            )


        val data = LineData(set)
        data.setValueTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.primaryTextColor
            )
        )
        data.setValueFormatter(MyValueFormatter())
        data.setValueTextSize(14f)

        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setDrawGridBackground(false)

        chart.setTouchEnabled(false)
        chart.isDragEnabled = false
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)
        chart.setExtraOffsets(30f, 10f, 30f, 20f)


        chart.axisLeft.setDrawGridLines(false)
        chart.axisRight.setDrawGridLines(false)
        chart.axisLeft.setDrawZeroLine(true)
        chart.axisLeft.isEnabled = false
        chart.axisLeft.spaceTop = 10f
        chart.axisLeft.spaceBottom = 20f
        chart.axisRight.isEnabled = false

        chart.xAxis.isEnabled = true
        chart.xAxis.textSize = 14f
        chart.xAxis.textColor = ContextCompat.getColor(
            requireContext(),
            R.color.primaryTextColor
        )
        chart.xAxis.valueFormatter = MyXAxisFormatter()
        chart.xAxis.setDrawAxisLine(true)
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.setLabelCount(chartData.count(), true)

        chart.data = data
        chart.invalidate()
    }

    private fun calculateChartData(balance: List<Balance>): List<Entry> {
        val incomePerYear = balance.map {
            it.date = it.date.split('-')[0]
            it
        }.groupingBy { item -> item.date }
            .fold(0f) { acc, el -> if (el.amount > 0) acc + el.amount else acc }

        val expensesPerYear = balance.map {
            it.date = it.date.split('-')[0]
            it
        }.groupingBy { item -> item.date }
            .fold(0f) { acc, el -> if (el.amount < 0) acc + el.amount else acc }

        return incomePerYear.map { (year, inc) ->
            Entry(
                year.toFloat(),
                inc - abs(expensesPerYear.getOrDefault(year, 0f))
            )
        }
    }
}
