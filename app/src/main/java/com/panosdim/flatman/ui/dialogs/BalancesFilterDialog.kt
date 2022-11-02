package com.panosdim.flatman.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.panosdim.flatman.R
import com.panosdim.flatman.databinding.DialogBalancesFilterBinding
import com.panosdim.flatman.model.Balance
import com.panosdim.flatman.model.BalanceFilters
import com.panosdim.flatman.model.Flat
import com.panosdim.flatman.ui.adapters.BalanceAdapter
import java.time.LocalDate

class BalancesFilterDialog :
    BottomSheetDialogFragment() {
    private var _binding: DialogBalancesFilterBinding? = null
    private val binding get() = _binding!!
    var flats: List<Flat> = mutableListOf()
    private var balancesList: List<Balance>? = null
    private var isFilterSelected: Boolean = false
    private val balanceFilters: BalanceFilters = BalanceFilters()
    var balanceAdapter: BalanceAdapter? = null
        set(value) {
            field = value
            if (balanceFilters.isFilterSet) {
                filter()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogBalancesFilterBinding.inflate(inflater, container, false)
        val root: View = binding.root

        flats.forEach { flat ->
            val chip = layoutInflater.inflate(
                R.layout.row_chip_view,
                root as ViewGroup,
                false
            ) as Chip
            chip.text = flat.name
            chip.id = flat.id!!

            binding.balancesFilterFlat.addView(chip)
        }

        binding.btnSetFilters.setOnClickListener {
            if (!balanceFilters.isFilterSet) {
                balancesList = balanceAdapter?.currentList?.map { it.copy() }?.toList()
            }
            balanceFilters.flat =
                if (binding.balancesFilterFlat.checkedChipId != View.NO_ID) binding.balancesFilterFlat.checkedChipId else null
            balanceFilters.balance =
                if (binding.balancesFilterBalance.checkedChipId != View.NO_ID) binding.balancesFilterBalance.checkedChipId else null
            balanceFilters.date =
                if (binding.balancesFilterDate.checkedChipId != View.NO_ID) binding.balancesFilterDate.checkedChipId else null

            filter()

            dismiss()
        }

        binding.balancesFilterFlat.setOnCheckedStateChangeListener { _, checkedIds ->
            isFilterSelected =
                binding.balancesFilterBalance.checkedChipId != View.NO_ID || binding.balancesFilterDate.checkedChipId != View.NO_ID || checkedIds.isNotEmpty()
            binding.btnSetFilters.isEnabled = isFilterSelected
        }

        binding.balancesFilterBalance.setOnCheckedStateChangeListener { _, checkedId ->
            isFilterSelected =
                binding.balancesFilterFlat.checkedChipId != View.NO_ID || binding.balancesFilterDate.checkedChipId != View.NO_ID || checkedId.isNotEmpty()
            binding.btnSetFilters.isEnabled = isFilterSelected
        }

        binding.balancesFilterDate.setOnCheckedStateChangeListener { _, checkedId ->
            isFilterSelected =
                binding.balancesFilterFlat.checkedChipId != View.NO_ID || binding.balancesFilterBalance.checkedChipId != View.NO_ID || checkedId.isNotEmpty()
            binding.btnSetFilters.isEnabled = isFilterSelected
        }

        binding.btnClearFilters.isEnabled = balanceFilters.isFilterSet
        binding.btnSetFilters.isEnabled = isFilterSelected

        binding.btnClearFilters.setOnClickListener {
            if (isFilterSelected) {
                isFilterSelected = false

                balanceAdapter?.submitList(balancesList)
                balancesList = listOf()
                balanceFilters.flat = null
                balanceFilters.balance = null
                balanceFilters.date = null
                binding.balancesFilterFlat.clearCheck()
                binding.balancesFilterBalance.clearCheck()
                binding.balancesFilterDate.clearCheck()
            }

            dismiss()
        }

        return root
    }

    private fun filter() {
        val filteredList = balancesList?.map { it.copy() }?.toMutableList()

        // Filter Flats
        balanceFilters.flat?.let { flatId ->
            filteredList?.retainAll { flatId == it.flatId }
        }

        // Filter Balance
        balanceFilters.balance?.let { chip ->
            when (chip) {
                R.id.expensesChip -> filteredList?.retainAll { it.amount < 0 }
                R.id.incomeChip -> filteredList?.retainAll { it.amount > 0 }
                else -> {}
            }
        }

        // Filter Date
        balanceFilters.date?.let { chip ->
            val today = LocalDate.now()
            when (chip) {
                R.id.previousMonth -> {
                    val previousMonth = today.minusMonths(1)
                    val startDate = previousMonth.withDayOfMonth(1)
                    val endDate = previousMonth.withDayOfMonth(previousMonth.lengthOfMonth())

                    filteredList?.retainAll {
                        val date = LocalDate.parse(it.date)

                        (date.isAfter(startDate) || date.isEqual(startDate))
                                && (date.isBefore(endDate) || date.isEqual(endDate))
                    }
                }
                R.id.lastSixMonths -> {
                    val startDate = today.minusMonths(6).withDayOfMonth(1)
                    val endDate = today.withDayOfMonth(today.lengthOfMonth())

                    filteredList?.retainAll {
                        val date = LocalDate.parse(it.date)

                        (date.isAfter(startDate) || date.isEqual(startDate))
                                && (date.isBefore(endDate) || date.isEqual(endDate))
                    }
                }
                else -> {}
            }
        }

        balanceAdapter?.submitList(filteredList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()

        // Restore filters that have been set
        if (balanceFilters.isFilterSet) {
            binding.balancesFilterBalance.clearCheck()
            binding.balancesFilterFlat.clearCheck()
            balanceFilters.balance?.let {
                binding.balancesFilterBalance.check(it)
            }
            balanceFilters.flat?.let {
                binding.balancesFilterFlat.check(it)
            }
            balanceFilters.date?.let {
                binding.balancesFilterDate.check(it)
            }
        }
    }

    companion object {
        const val TAG = "BalancesFilterDialog"
    }
}