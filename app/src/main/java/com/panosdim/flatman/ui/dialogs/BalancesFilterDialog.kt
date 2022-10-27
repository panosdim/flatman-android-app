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
import com.panosdim.flatman.model.Flat
import com.panosdim.flatman.ui.adapters.BalanceAdapter

class BalancesFilterDialog :
    BottomSheetDialogFragment() {
    private var _binding: DialogBalancesFilterBinding? = null
    private val binding get() = _binding!!
    var flats: List<Flat> = mutableListOf()
    private var balancesList: List<Balance>? = null
    private var isFilterSet: Boolean = false
    private var filters: List<Int> = mutableListOf()
    var balanceAdapter: BalanceAdapter? = null
        set(value) {
            field = value
            if (isFilterSet) {
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
            if (!isFilterSet) {
                balancesList = balanceAdapter?.currentList?.map { it.copy() }?.toList()
            }
            filters = binding.balancesFilterFlat.checkedChipIds
            isFilterSet = true
            filter()

            dismiss()
        }

        binding.balancesFilterFlat.setOnCheckedStateChangeListener { _, checkedIds ->
            binding.btnSetFilters.isEnabled = checkedIds.isNotEmpty()
        }

        binding.btnClearFilters.isEnabled = isFilterSet
        binding.btnSetFilters.isEnabled = isFilterSet

        binding.btnClearFilters.setOnClickListener {
            if (isFilterSet) {
                isFilterSet = false

                balanceAdapter?.submitList(balancesList)
                balancesList = listOf()
                filters = listOf()
                binding.balancesFilterFlat.clearCheck()
            }

            dismiss()
        }

        return root
    }

    private fun filter() {
        val filteredList = balancesList?.map { it.copy() }?.toMutableList()
        filteredList?.retainAll { filters.contains(it.flatId) }
        balanceAdapter?.submitList(filteredList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "BalancesFilterDialog"
    }
}