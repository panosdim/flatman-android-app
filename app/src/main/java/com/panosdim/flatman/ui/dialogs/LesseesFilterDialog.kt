package com.panosdim.flatman.ui.dialogs

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.panosdim.flatman.R
import com.panosdim.flatman.databinding.DialogLesseesFilterBinding
import com.panosdim.flatman.model.Flat

class LesseesFilterDialog(private val onFilterChangedCallback: (List<Int>) -> Unit) :
    BottomSheetDialogFragment() {
    private var _binding: DialogLesseesFilterBinding? = null
    private val binding get() = _binding!!
    var flats: List<Flat> = mutableListOf()
    var filters: List<Int> = mutableListOf()

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

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        filters = binding.lesseesFilterFlat.checkedChipIds
        onFilterChangedCallback(binding.lesseesFilterFlat.checkedChipIds)
    }

    companion object {
        const val TAG = "LesseesFilterDialog"
    }
}