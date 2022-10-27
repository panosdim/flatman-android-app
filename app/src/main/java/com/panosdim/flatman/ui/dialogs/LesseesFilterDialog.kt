package com.panosdim.flatman.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.panosdim.flatman.R
import com.panosdim.flatman.databinding.DialogLesseesFilterBinding
import com.panosdim.flatman.model.Flat
import com.panosdim.flatman.model.Lessee
import com.panosdim.flatman.ui.adapters.LesseesAdapter

class LesseesFilterDialog() :
    BottomSheetDialogFragment() {
    private var _binding: DialogLesseesFilterBinding? = null
    private val binding get() = _binding!!
    var flats: List<Flat> = mutableListOf()
    private var lesseesList: List<Lessee>? = null
    private var isFilterSet: Boolean = false
    private var filters: List<Int> = mutableListOf()
    var lesseesAdapter: LesseesAdapter? = null
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
        _binding = DialogLesseesFilterBinding.inflate(inflater, container, false)
        val root: View = binding.root

        flats.forEach { flat ->
            val chip = layoutInflater.inflate(
                R.layout.row_chip_view,
                root as ViewGroup,
                false
            ) as Chip
            chip.text = flat.name
            chip.id = flat.id!!

            binding.lesseesFilterFlat.addView(chip)
        }

        binding.btnSetFilters.setOnClickListener {
            if (!isFilterSet) {
                lesseesList = lesseesAdapter?.currentList?.map { it.copy() }?.toList()
            }
            filters = binding.lesseesFilterFlat.checkedChipIds
            isFilterSet = true
            filter()

            dismiss()
        }

        binding.lesseesFilterFlat.setOnCheckedStateChangeListener { _, checkedIds ->
            binding.btnSetFilters.isEnabled = checkedIds.isNotEmpty()
        }

        binding.btnClearFilters.isEnabled = isFilterSet
        binding.btnSetFilters.isEnabled = isFilterSet

        binding.btnClearFilters.setOnClickListener {
            if (isFilterSet) {
                isFilterSet = false

                lesseesAdapter?.submitList(lesseesList)
                lesseesList = listOf()
                filters = listOf()
                binding.lesseesFilterFlat.clearCheck()
            }

            dismiss()
        }

        return root
    }

    private fun filter() {
        val filteredList = lesseesList?.map { it.copy() }?.toMutableList()
        filteredList?.retainAll { filters.contains(it.flatId) }
        lesseesAdapter?.submitList(filteredList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "LesseesFilterDialog"
    }
}