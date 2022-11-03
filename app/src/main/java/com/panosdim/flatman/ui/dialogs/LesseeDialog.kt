package com.panosdim.flatman.ui.dialogs

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.panosdim.flatman.R
import com.panosdim.flatman.api.data.Resource
import com.panosdim.flatman.databinding.DialogLesseeBinding
import com.panosdim.flatman.model.Flat
import com.panosdim.flatman.model.Lessee
import com.panosdim.flatman.utils.*
import com.panosdim.flatman.viewmodel.LesseeViewModel
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class LesseeDialog : BottomSheetDialogFragment() {
    private var _binding: DialogLesseeBinding? = null
    private val binding get() = _binding!!
    private var lessee: Lessee? = null
    private var selectedFlat: Flat? = null
    private val textWatcher = generateTextWatcher(::validateForm)
    private val viewModel: LesseeViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private val postalCodeRegex = """^[12345678][0-9]{4}$""".toRegex()
    var flats: List<Flat> = mutableListOf()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogLesseeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.lesseeRent.setOnEditorActionListener { _, actionId, event ->
            if (isFormValid() && (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE)) {
                lessee?.let {
                    updateLessee(it)
                } ?: kotlin.run {
                    saveLessee()
                }
            }
            false
        }

        binding.lesseeFrom.setOnClickListener {
            val date: LocalDate = binding.lesseeFrom.text.toString().toLocalDate()
            val untilDate: LocalDate = binding.lesseeUntil.text.toString().toLocalDate()

            //Date Picker
            val builder = MaterialDatePicker.Builder.datePicker()
            val constraintsBuilder = CalendarConstraints.Builder()
            constraintsBuilder.setEnd(untilDate.toEpochMilli())
            constraintsBuilder.setOpenAt(date.toEpochMilli())
            builder.setCalendarConstraints(constraintsBuilder.build())
            builder.setSelection(date.toEpochMilli())
            builder.setTitleText("Select From Date")

            val picker: MaterialDatePicker<Long> = builder.build()
            picker.addOnPositiveButtonClickListener { selection ->
                binding.lesseeFrom.setText(fromEpochMilli(selection).toShowDateFormat())
            }

            picker.show(childFragmentManager, picker.toString())
        }

        binding.lesseeUntil.setOnClickListener {
            val date: LocalDate = binding.lesseeUntil.text.toString().toLocalDate()
            val fromDate: LocalDate = binding.lesseeFrom.text.toString().toLocalDate()

            //Date Picker
            val builder = MaterialDatePicker.Builder.datePicker()
            val constraintsBuilder = CalendarConstraints.Builder()
            constraintsBuilder.setStart(fromDate.toEpochMilli())
            constraintsBuilder.setOpenAt(date.toEpochMilli())
            builder.setCalendarConstraints(constraintsBuilder.build())
            builder.setSelection(date.toEpochMilli())
            builder.setTitleText("Select Until Date")

            val picker: MaterialDatePicker<Long> = builder.build()
            picker.addOnPositiveButtonClickListener { selection ->
                binding.lesseeUntil.setText(fromEpochMilli(selection).toShowDateFormat())
            }

            picker.show(childFragmentManager, picker.toString())
        }

        binding.saveLessee.setOnClickListener {
            binding.prgIndicator.visibility = View.VISIBLE
            binding.saveLessee.isEnabled = false
            binding.deleteLessee.isEnabled = false

            lessee?.let {
                updateLessee(it)
            } ?: kotlin.run {
                saveLessee()
            }
        }

        binding.deleteLessee.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(resources.getString(R.string.delete_lessee_title))
                .setMessage(resources.getString(R.string.delete_lessee_supporting_text))
                .setNegativeButton(resources.getString(R.string.decline)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(resources.getString(R.string.accept)) { dialog, _ ->
                    dialog.dismiss()
                    deleteLessee()
                }
                .show()
        }

        binding.lesseeAddress.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && !binding.lesseeAddress.text.isNullOrEmpty() && binding.lesseePostalCode.text.isNullOrEmpty()) {
                val response = viewModel.getPostalCode(binding.lesseeAddress.text.toString())
                if (response != null && response.TK != "NOT FOUND") {
                    binding.lesseePostalCode.setText(response.TK.replace("\\s+".toRegex(), ""))
                }
            }
        }

        binding.flat.setOnItemClickListener { parent, _, position, _ ->
            selectedFlat = (parent.getItemAtPosition(position) as Flat)
            validateForm()
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.setOnShowListener { dialog ->
            val d = dialog as BottomSheetDialog
            val bottomSheet = d.findViewById<View>(R.id.design_bottom_sheet) as FrameLayout
            val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPause() {
        super.onPause()
        lessee = null
        // Workaround as there is a bug in AutoCompleteView filter setting
        binding.flat.setText("", false)
        selectedFlat = null
    }

    private fun saveLessee() {
        val newLessee = Lessee(
            null,
            binding.lesseeName.text.toString(),
            binding.lesseeAddress.text.toString(),
            binding.lesseePostalCode.text.toString(),
            binding.lesseeFrom.text.toString().toSQLDateFormat(),
            binding.lesseeUntil.text.toString().toSQLDateFormat(),
            selectedFlat?.id!!,
            binding.lesseeRent.text.toString().toInt(),
            binding.lesseeTIN.text.toString()
        )

        viewModel.addLessee(newLessee).observe(viewLifecycleOwner) { resource ->
            if (resource != null) {
                when (resource) {
                    is Resource.Success -> {
                        dismiss()
                        binding.prgIndicator.visibility = View.GONE
                        binding.deleteLessee.isEnabled = true
                        binding.saveLessee.isEnabled = true
                    }
                    is Resource.Error -> {
                        Toast.makeText(
                            requireContext(),
                            resource.message,
                            Toast.LENGTH_LONG
                        ).show()
                        binding.prgIndicator.visibility = View.GONE
                        binding.deleteLessee.isEnabled = true
                        binding.saveLessee.isEnabled = true
                    }
                    is Resource.Loading -> {
                        binding.prgIndicator.visibility = View.VISIBLE
                        binding.deleteLessee.isEnabled = false
                        binding.saveLessee.isEnabled = false
                    }
                }
            }
        }
    }

    private fun deleteLessee() {
        lessee?.let {
            viewModel.removeLessee(it).observe(viewLifecycleOwner) { resource ->
                if (resource != null) {
                    when (resource) {
                        is Resource.Success -> {
                            dismiss()
                            binding.prgIndicator.visibility = View.GONE
                            binding.deleteLessee.isEnabled = true
                            binding.saveLessee.isEnabled = true
                        }
                        is Resource.Error -> {
                            Toast.makeText(
                                requireContext(),
                                resource.message,
                                Toast.LENGTH_LONG
                            ).show()
                            binding.prgIndicator.visibility = View.GONE
                            binding.deleteLessee.isEnabled = true
                            binding.saveLessee.isEnabled = true
                        }
                        is Resource.Loading -> {
                            binding.prgIndicator.visibility = View.VISIBLE
                            binding.deleteLessee.isEnabled = false
                            binding.saveLessee.isEnabled = false
                        }
                    }
                }
            }
        }
    }


    private fun updateLessee(lessee: Lessee) {
        // Check if we change something in the object
        if (lessee.name == binding.lesseeName.text.toString() &&
            lessee.address == binding.lesseeAddress.text.toString() &&
            lessee.postalCode == binding.lesseePostalCode.text.toString() &&
            lessee.from == binding.lesseeFrom.text.toString().toSQLDateFormat() &&
            lessee.until == binding.lesseeUntil.text.toString().toSQLDateFormat() &&
            lessee.rent == binding.lesseeRent.text.toString().toInt() &&
            lessee.tin == binding.lesseeTIN.text.toString() &&
            lessee.flatId == selectedFlat?.id!!
        ) {
            dismiss()
        } else {
            // Update Lessee
            val updatedLessee = lessee.copy(
                name = binding.lesseeName.text.toString(),
                address = binding.lesseeAddress.text.toString(),
                postalCode = binding.lesseePostalCode.text.toString(),
                from = binding.lesseeFrom.text.toString().toSQLDateFormat(),
                until = binding.lesseeUntil.text.toString().toSQLDateFormat(),
                flatId = selectedFlat?.id!!,
                rent = binding.lesseeRent.text.toString().toInt(),
                tin = binding.lesseeTIN.text.toString()
            )

            viewModel.updateLessee(updatedLessee).observe(viewLifecycleOwner) { resource ->
                if (resource != null) {
                    when (resource) {
                        is Resource.Success -> {
                            dismiss()
                            binding.prgIndicator.visibility = View.GONE
                            binding.deleteLessee.isEnabled = true
                            binding.saveLessee.isEnabled = true
                        }
                        is Resource.Error -> {
                            Toast.makeText(
                                requireContext(),
                                resource.message,
                                Toast.LENGTH_LONG
                            ).show()
                            binding.prgIndicator.visibility = View.GONE
                            binding.deleteLessee.isEnabled = true
                            binding.saveLessee.isEnabled = true
                        }
                        is Resource.Loading -> {
                            binding.prgIndicator.visibility = View.VISIBLE
                            binding.deleteLessee.isEnabled = false
                            binding.saveLessee.isEnabled = false
                        }
                    }
                }
            }
        }
    }

    private fun validateForm() {
        val lesseeName = binding.lesseeName
        val lesseeAddress = binding.lesseeAddress
        val lesseePostalCode = binding.lesseePostalCode
        val lesseeTIN = binding.lesseeTIN
        val lesseeRent = binding.lesseeRent
        val flat = binding.flat
        val saveLessee = binding.saveLessee

        saveLessee.isEnabled = true
        lesseeName.error = null
        lesseeAddress.error = null
        lesseePostalCode.error = null
        lesseeTIN.error = null
        lesseeRent.error = null
        flat.error = null

        // Store values.
        val name = lesseeName.text.toString()
        val address = lesseeAddress.text.toString()
        val postalCode = lesseePostalCode.text.toString()
        val tin = lesseeTIN.text.toString()
        val rent = lesseeRent.text.toString()

        if (name.isEmpty()) {
            lesseeName.error = getString(R.string.error_field_required)
            saveLessee.isEnabled = false
        }

        if (address.isEmpty()) {
            lesseeAddress.error = getString(R.string.error_field_required)
            saveLessee.isEnabled = false
        }

        if (postalCode.isEmpty()) {
            lesseePostalCode.error = getString(R.string.error_field_required)
            saveLessee.isEnabled = false
        } else {
            if (!postalCodeRegex.matches(postalCode)) {
                lesseePostalCode.error = getString(R.string.error_postal_code)
                saveLessee.isEnabled = false
            }
        }

        if (tin.isEmpty()) {
            lesseeTIN.error = getString(R.string.error_field_required)
            saveLessee.isEnabled = false
        }

        if (tin.length == 9) {
            if (!isValidTIN(tin)) {
                lesseeTIN.error = getString(R.string.error_tin)
                saveLessee.isEnabled = false
            }
        } else {
            lesseeTIN.error = getString(R.string.error_tin_length)
            saveLessee.isEnabled = false
        }

        if (rent.isEmpty()) {
            lesseeRent.error = getString(R.string.error_field_required)
            saveLessee.isEnabled = false
        }

        if (selectedFlat == null) {
            flat.error = getString(R.string.error_field_required)
            saveLessee.isEnabled = false
        }
    }

    private fun isFormValid(): Boolean {
        return binding.lesseeName.error == null &&
                binding.lesseeAddress.error == null &&
                binding.lesseePostalCode.error == null &&
                binding.lesseeTIN.error == null &&
                binding.lesseeRent.error == null &&
                binding.flat.error == null
    }

    fun showForm(lesseeItm: Lessee?) {
        val adapter = ArrayAdapter(requireContext(), R.layout.list_item, flats)
        binding.flat.setAdapter(adapter)

        binding.prgIndicator.visibility = View.GONE
        binding.saveLessee.isEnabled = true
        binding.deleteLessee.isEnabled = true

        binding.lesseeName.removeTextChangedListener(textWatcher)
        binding.lesseeAddress.removeTextChangedListener(textWatcher)
        binding.lesseePostalCode.removeTextChangedListener(textWatcher)
        binding.lesseeTIN.removeTextChangedListener(textWatcher)
        binding.lesseeRent.removeTextChangedListener(textWatcher)
        binding.lesseeName.error = null
        binding.lesseeAddress.error = null
        binding.lesseePostalCode.error = null
        binding.lesseeTIN.error = null
        binding.lesseeRent.error = null
        binding.flat.error = null

        lessee = lesseeItm
        if (lesseeItm == null) {
            binding.lesseeName.addTextChangedListener(textWatcher)
            binding.lesseeAddress.addTextChangedListener(textWatcher)
            binding.lesseePostalCode.addTextChangedListener(textWatcher)
            binding.lesseeTIN.addTextChangedListener(textWatcher)
            binding.lesseeRent.addTextChangedListener(textWatcher)
            val from = LocalDate.now().with(TemporalAdjusters.firstDayOfNextMonth())
            val until = from.plusYears(1).minusMonths(1).with(TemporalAdjusters.lastDayOfMonth())
            binding.lesseeName.setText("")
            binding.lesseeAddress.setText("")
            binding.lesseePostalCode.setText("")
            binding.lesseeTIN.setText("")
            binding.lesseeRent.setText("")
            binding.lesseeFrom.setText(from.toShowDateFormat())
            binding.lesseeUntil.setText(until.toShowDateFormat())
            binding.deleteLessee.visibility = View.GONE
            binding.saveLessee.setText(R.string.save)
            binding.flat.requestFocus()
        } else {
            binding.lesseeName.setText(lesseeItm.name)
            binding.lesseeAddress.setText(lesseeItm.address)
            binding.lesseePostalCode.setText(lesseeItm.postalCode)
            binding.lesseeTIN.setText(lesseeItm.tin)
            binding.lesseeRent.setText(lesseeItm.rent.toString())
            binding.lesseeFrom.setText(LocalDate.parse(lesseeItm.from).toShowDateFormat())
            binding.lesseeUntil.setText(LocalDate.parse(lesseeItm.until).toShowDateFormat())
            selectedFlat = flats.firstOrNull { it.id == lesseeItm.flatId }
            binding.flat.setText(selectedFlat!!.name, false)
            binding.lesseeName.clearFocus()
            binding.lesseeAddress.clearFocus()
            binding.lesseePostalCode.clearFocus()
            binding.lesseeTIN.clearFocus()
            binding.lesseeRent.clearFocus()
            binding.lesseeFrom.clearFocus()
            binding.lesseeUntil.clearFocus()
            binding.deleteLessee.visibility = View.VISIBLE
            binding.saveLessee.setText(R.string.update)
            binding.lesseeName.addTextChangedListener(textWatcher)
            binding.lesseeAddress.addTextChangedListener(textWatcher)
            binding.lesseePostalCode.addTextChangedListener(textWatcher)
            binding.lesseeTIN.addTextChangedListener(textWatcher)
            binding.lesseeRent.addTextChangedListener(textWatcher)
        }
    }

    companion object {
        const val TAG = "LesseeDialog"
    }
}