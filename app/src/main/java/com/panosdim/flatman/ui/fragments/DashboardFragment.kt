package com.panosdim.flatman.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.panosdim.flatman.R
import com.panosdim.flatman.api.data.Resource
import com.panosdim.flatman.databinding.FragmentDashboardBinding
import com.panosdim.flatman.model.Balance
import com.panosdim.flatman.utils.MyValueFormatter
import com.panosdim.flatman.utils.MyXAxisFormatter
import com.panosdim.flatman.utils.moneyFormat
import com.panosdim.flatman.utils.resolveColorAttr
import com.panosdim.flatman.viewmodel.BalanceViewModel
import kotlin.math.abs


class DashboardFragment : Fragment() {
    private lateinit var chartData: List<Entry>
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BalanceViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        viewModel.getBalance().observe(viewLifecycleOwner) { resource ->
            if (resource != null) {
                when (resource) {
                    is Resource.Success -> {
                        resource.data?.let {
                            if (it.isNotEmpty()) {
                                updateDashboard(it, binding.chart)
                            }
                        }
                        binding.progressBar.visibility = View.GONE
                        binding.llDashboard.visibility = View.VISIBLE
                    }
                    is Resource.Error -> {
                        Toast.makeText(
                            requireContext(),
                            resource.message,
                            Toast.LENGTH_LONG
                        ).show()
                        binding.progressBar.visibility = View.GONE
                        binding.llDashboard.visibility = View.VISIBLE
                    }
                    is Resource.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.llDashboard.visibility = View.GONE
                    }
                }
            }
        }

        return root
    }

    private fun updateDashboard(
        bal: List<Balance>,
        chart: LineChart
    ) {
        val balList = bal.map { it.copy() }
        val totInc = balList.map { it.amount }.reduce { tot, next ->
            if (next > 0) tot + next else tot
        }

        val totExp = balList.map { it.amount }.reduce { tot, next ->
            if (next < 0) tot + next else tot
        }
        val totSav = totInc - abs(totExp)
        binding.totalIncome.text = moneyFormat(totInc)
        binding.totalExpenses.text = moneyFormat(abs(totExp))
        binding.totalSavings.text = moneyFormat(totSav)

        chartData = calculateChartData(balList)
        initializeChart(chart)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initializeChart(chart: LineChart) {
        val set = LineDataSet(chartData, "Savings Per Year")
        set.color = requireContext().resolveColorAttr(R.attr.colorSavings)
        set.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        set.cubicIntensity = 0.05f
        set.setCircleColor(requireContext().resolveColorAttr(R.attr.colorTertiary))
        set.circleHoleColor = requireContext().resolveColorAttr(R.attr.colorPrimary)

        val data = LineData(set)
        data.setValueTextColor(requireContext().resolveColorAttr(R.attr.colorOnSurface))
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
        chart.xAxis.textColor = requireContext().resolveColorAttr(R.attr.colorOnSurface)
        chart.xAxis.valueFormatter = MyXAxisFormatter()
        chart.xAxis.setDrawAxisLine(true)
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.setLabelCount(chartData.count(), true)

        chart.data = data
        chart.invalidate()
    }

    private fun calculateChartData(balance: List<Balance>): List<Entry> {
        val incomePerYear = balance.map {
            it.copy(date = it.date.split('-')[0])
        }.groupingBy { item -> item.date }
            .fold(0f) { acc, el -> if (el.amount > 0) acc + el.amount else acc }

        val expensesPerYear = balance.map {
            it.copy(date = it.date.split('-')[0])
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
