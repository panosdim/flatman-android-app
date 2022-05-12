package com.panosdim.flatman.ui.fragments

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.panosdim.flatman.R
import com.panosdim.flatman.api.data.Resource
import com.panosdim.flatman.model.Flat
import com.panosdim.flatman.model.Lessee
import com.panosdim.flatman.ui.adapters.LesseesAdapter
import com.panosdim.flatman.utils.*
import com.panosdim.flatman.viewmodel.FlatViewModel
import com.panosdim.flatman.viewmodel.LesseeViewModel
import kotlinx.android.synthetic.main.dialog_lessee.view.*
import kotlinx.android.synthetic.main.fragment_lessees.*
import kotlinx.android.synthetic.main.fragment_lessees.view.*
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters


class LesseesFragment : Fragment() {
    private val flatViewModel: FlatViewModel by viewModels()
    private lateinit var dialog: BottomSheetDialog
    private lateinit var dialogView: View
    private lateinit var rootView: View
    private var lessee: Lessee? = null
    private var selectedFlat: Flat? = null
    private lateinit var flatSelectAdapter: ArrayAdapter<Flat>
    private val postalCodeRegex = """^[12345678][0-9]{4}$""".toRegex()
    private val viewModel: LesseeViewModel by viewModels()
    private val textWatcher = generateTextWatcher(::validateForm)

    private fun lesseeItemClicked(lesseeItem: Lessee) {
        lessee = lesseeItem
        showForm(lessee)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getAllLessee().observe(viewLifecycleOwner) { resource ->
            if (resource != null) {
                when (resource) {
                    is Resource.Success -> {
                        progress_bar.visibility = View.GONE
                        rvLessees.visibility = View.VISIBLE
                        updateLesseeAdapter()
                        viewModel.getAllLessee().removeObservers(viewLifecycleOwner)
                    }
                    is Resource.Error -> {
                        Toast.makeText(
                            requireContext(),
                            resource.message,
                            Toast.LENGTH_LONG
                        ).show()
                        progress_bar.visibility = View.GONE
                        rvLessees.visibility = View.VISIBLE
                    }
                    is Resource.Loading -> {
                        progress_bar.visibility = View.VISIBLE
                        rvLessees.visibility = View.GONE
                    }
                }
            }
        }

        viewModel.lessee.observe(viewLifecycleOwner) {
            updateLesseeAdapter()
        }

        swipe_refresh.setOnRefreshListener {
            viewModel.refreshLessee()
            swipe_refresh.isRefreshing = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_lessees, container, false)
        dialog = BottomSheetDialog(requireContext())
        dialogView = inflater.inflate(R.layout.dialog_lessee, container, false)
        dialog.setContentView(dialogView)
        val displayMetrics = requireActivity().resources.displayMetrics
        val height = displayMetrics.heightPixels
        val maxHeight = (height * 0.88).toInt()

        val mBehavior: BottomSheetBehavior<*> =
            BottomSheetBehavior.from(dialogView.parent as View)
        mBehavior.peekHeight = maxHeight

        val rvLessees = rootView.rvLessees
        rvLessees.setHasFixedSize(true)
        rvLessees.layoutManager = LinearLayoutManager(rootView.context)

        rootView.addNewLessee.setOnClickListener {
            lessee = null
            showForm(lessee)
        }

        dialogView.lesseeRent.setOnEditorActionListener { _, actionId, event ->
            if (isFormValid() && (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE)) {
                lessee?.let {
                    updateLessee(it)
                } ?: kotlin.run {
                    saveLessee()
                }
            }
            false
        }

        dialogView.lesseeFrom.setOnClickListener {
            val date: LocalDate = dialogView.lesseeFrom.text.toString().toLocalDate()
            val untilDate: LocalDate = dialogView.lesseeUntil.text.toString().toLocalDate()

            //Date Picker
            val builder = MaterialDatePicker.Builder.datePicker()
            val constraintsBuilder = CalendarConstraints.Builder()
            constraintsBuilder.setEnd(untilDate.toEpochMilli())
            constraintsBuilder.setOpenAt(date.toEpochMilli())
            builder.setCalendarConstraints(constraintsBuilder.build())
            builder.setTheme(R.style.Theme_FlatMan_MaterialCalendar)
            builder.setSelection(date.toEpochMilli())
            builder.setTitleText("Select From Date")

            val picker: MaterialDatePicker<Long> = builder.build()
            picker.addOnPositiveButtonClickListener { selection ->
                dialogView.lesseeFrom.setText(fromEpochMilli(selection).toShowDateFormat())
            }

            picker.show(childFragmentManager, picker.toString())
        }

        dialogView.lesseeUntil.setOnClickListener {
            val date: LocalDate = dialogView.lesseeUntil.text.toString().toLocalDate()
            val fromDate: LocalDate = dialogView.lesseeFrom.text.toString().toLocalDate()

            //Date Picker
            val builder = MaterialDatePicker.Builder.datePicker()
            val constraintsBuilder = CalendarConstraints.Builder()
            constraintsBuilder.setStart(fromDate.toEpochMilli())
            constraintsBuilder.setOpenAt(date.toEpochMilli())
            builder.setCalendarConstraints(constraintsBuilder.build())
            builder.setTheme(R.style.Theme_FlatMan_MaterialCalendar)
            builder.setSelection(date.toEpochMilli())
            builder.setTitleText("Select Until Date")

            val picker: MaterialDatePicker<Long> = builder.build()
            picker.addOnPositiveButtonClickListener { selection ->
                dialogView.lesseeUntil.setText(fromEpochMilli(selection).toShowDateFormat())
            }

            picker.show(childFragmentManager, picker.toString())
        }

        dialogView.saveLessee.setOnClickListener {
            dialogView.prgIndicator.visibility = View.VISIBLE
            dialogView.saveLessee.isEnabled = false
            dialogView.deleteLessee.isEnabled = false

            lessee?.let {
                updateLessee(it)
            } ?: kotlin.run {
                saveLessee()
            }
        }

        dialogView.deleteLessee.setOnClickListener {
            deleteLessee()
        }

        dialogView.lesseeAddress.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && !dialogView.lesseeAddress.text.isNullOrEmpty() && dialogView.lesseePostalCode.text.isNullOrEmpty()) {
                val response = viewModel.getPostalCode(dialogView.lesseeAddress.text.toString())
                if (response != null && response.TK != "NOT FOUND") {
                    dialogView.lesseePostalCode.setText(response.TK.replace("\\s+".toRegex(), ""))
                }
            }
        }

        flatViewModel.flats.observe(viewLifecycleOwner) {
            if (it != null && it.isNotEmpty()) {
                rootView.addNewLessee.isEnabled = true
                flatSelectAdapter = ArrayAdapter(requireContext(), R.layout.list_item, it)
                rootView.selectedFlat.setAdapter(flatSelectAdapter)
                selectedFlat = flatSelectAdapter.getItem(0)
                rootView.selectedFlat.setText(selectedFlat.toString(), false)

                updateLesseeAdapter()
            } else {
                rootView.addNewLessee.isEnabled = false
                rootView.rvLessees.adapter =
                    LesseesAdapter(mutableListOf()) { lesseeItem: Lessee ->
                        lesseeItemClicked(
                            lesseeItem
                        )
                    }
            }
        }

        rootView.selectedFlat.setOnItemClickListener { _, _, position, _ ->
            selectedFlat = flatSelectAdapter.getItem(position)
            updateLesseeAdapter()
        }

        return rootView
    }

    private fun updateLesseeAdapter() {
        val data = viewModel.lessee.value
        if (data != null) {
            val temp = data.filter {
                it.flatId == (selectedFlat?.id ?: -1)
            }
            rootView.rvLessees.adapter =
                LesseesAdapter(temp) { lesseeItem: Lessee -> lesseeItemClicked(lesseeItem) }
            (rootView.rvLessees.adapter as LesseesAdapter).notifyDataSetChanged()
        } else {
            rootView.rvLessees.adapter =
                LesseesAdapter(mutableListOf()) { lesseeItem: Lessee -> lesseeItemClicked(lesseeItem) }
            (rootView.rvLessees.adapter as LesseesAdapter).notifyDataSetChanged()
        }
    }

    private fun deleteLessee() {
        lessee?.let {
            viewModel.removeLessee(it).observe(viewLifecycleOwner) { resource ->
                if (resource != null) {
                    when (resource) {
                        is Resource.Success -> {
                            dialog.hide()
                            dialogView.prgIndicator.visibility = View.GONE
                            dialogView.deleteLessee.isEnabled = true
                            dialogView.saveLessee.isEnabled = true
                        }
                        is Resource.Error -> {
                            Toast.makeText(
                                requireContext(),
                                resource.message,
                                Toast.LENGTH_LONG
                            ).show()
                            dialogView.prgIndicator.visibility = View.GONE
                            dialogView.deleteLessee.isEnabled = true
                            dialogView.saveLessee.isEnabled = true
                        }
                        is Resource.Loading -> {
                            dialogView.prgIndicator.visibility = View.VISIBLE
                            dialogView.deleteLessee.isEnabled = false
                            dialogView.saveLessee.isEnabled = false
                        }
                    }
                }
            }
        }
    }

    private fun saveLessee() {
        val newLessee = Lessee(
            null,
            dialogView.lesseeName.text.toString(),
            dialogView.lesseeAddress.text.toString(),
            dialogView.lesseePostalCode.text.toString(),
            dialogView.lesseeFrom.text.toString().toSQLDateFormat(),
            dialogView.lesseeUntil.text.toString().toSQLDateFormat(),
            selectedFlat?.id!!,
            dialogView.lesseeRent.text.toString().toInt(),
            dialogView.lesseeTIN.text.toString()
        )

        viewModel.addLessee(newLessee).observe(viewLifecycleOwner) { resource ->
            if (resource != null) {
                when (resource) {
                    is Resource.Success -> {
                        dialog.hide()
                        dialogView.prgIndicator.visibility = View.GONE
                        dialogView.deleteLessee.isEnabled = true
                        dialogView.saveLessee.isEnabled = true
                    }
                    is Resource.Error -> {
                        Toast.makeText(
                            requireContext(),
                            resource.message,
                            Toast.LENGTH_LONG
                        ).show()
                        dialogView.prgIndicator.visibility = View.GONE
                        dialogView.deleteLessee.isEnabled = true
                        dialogView.saveLessee.isEnabled = true
                    }
                    is Resource.Loading -> {
                        dialogView.prgIndicator.visibility = View.VISIBLE
                        dialogView.deleteLessee.isEnabled = false
                        dialogView.saveLessee.isEnabled = false
                    }
                }
            }
        }
    }

    private fun updateLessee(lessee: Lessee) {
        // Check if we change something in the object
        if (lessee.name == dialogView.lesseeName.text.toString() &&
            lessee.address == dialogView.lesseeAddress.text.toString() &&
            lessee.postalCode == dialogView.lesseePostalCode.text.toString() &&
            lessee.from == dialogView.lesseeFrom.text.toString().toSQLDateFormat() &&
            lessee.until == dialogView.lesseeUntil.text.toString().toSQLDateFormat() &&
            lessee.rent == dialogView.lesseeRent.text.toString().toInt() &&
            lessee.tin == dialogView.lesseeTIN.text.toString()
        ) {
            dialog.hide()
        } else {
            // Update Lessee
            lessee.name = dialogView.lesseeName.text.toString()
            lessee.address = dialogView.lesseeAddress.text.toString()
            lessee.postalCode = dialogView.lesseePostalCode.text.toString()
            lessee.from = dialogView.lesseeFrom.text.toString().toSQLDateFormat()
            lessee.until = dialogView.lesseeUntil.text.toString().toSQLDateFormat()
            lessee.rent = dialogView.lesseeRent.text.toString().toInt()
            lessee.tin = dialogView.lesseeTIN.text.toString()

            viewModel.updateLessee(lessee).observe(viewLifecycleOwner) { resource ->
                if (resource != null) {
                    when (resource) {
                        is Resource.Success -> {
                            dialog.hide()
                            dialogView.prgIndicator.visibility = View.GONE
                            dialogView.deleteLessee.isEnabled = true
                            dialogView.saveLessee.isEnabled = true
                        }
                        is Resource.Error -> {
                            Toast.makeText(
                                requireContext(),
                                resource.message,
                                Toast.LENGTH_LONG
                            ).show()
                            dialogView.prgIndicator.visibility = View.GONE
                            dialogView.deleteLessee.isEnabled = true
                            dialogView.saveLessee.isEnabled = true
                        }
                        is Resource.Loading -> {
                            dialogView.prgIndicator.visibility = View.VISIBLE
                            dialogView.deleteLessee.isEnabled = false
                            dialogView.saveLessee.isEnabled = false
                        }
                    }
                }
            }
        }
    }

    private fun validateForm() {
        val lesseeName = dialogView.lesseeName
        val lesseeAddress = dialogView.lesseeAddress
        val lesseePostalCode = dialogView.lesseePostalCode
        val lesseeTIN = dialogView.lesseeTIN
        val lesseeRent = dialogView.lesseeRent
        val saveLessee = dialogView.saveLessee

        saveLessee.isEnabled = true
        lesseeName.error = null
        lesseeAddress.error = null
        lesseePostalCode.error = null
        lesseeTIN.error = null
        lesseeRent.error = null

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
    }

    private fun isFormValid(): Boolean {
        return dialogView.lesseeName.error == null &&
                dialogView.lesseeAddress.error == null &&
                dialogView.lesseePostalCode.error == null &&
                dialogView.lesseeTIN.error == null &&
                dialogView.lesseeRent.error == null
    }

    private fun showForm(lessee: Lessee?) {
        dialogView.prgIndicator.visibility = View.GONE
        dialogView.saveLessee.isEnabled = true
        dialogView.deleteLessee.isEnabled = true

        dialogView.lesseeName.removeTextChangedListener(textWatcher)
        dialogView.lesseeAddress.removeTextChangedListener(textWatcher)
        dialogView.lesseePostalCode.removeTextChangedListener(textWatcher)
        dialogView.lesseeTIN.removeTextChangedListener(textWatcher)
        dialogView.lesseeRent.removeTextChangedListener(textWatcher)
        dialogView.lesseeName.error = null
        dialogView.lesseeAddress.error = null
        dialogView.lesseePostalCode.error = null
        dialogView.lesseeTIN.error = null
        dialogView.lesseeRent.error = null


        if (lessee == null) {
            dialogView.lesseeName.addTextChangedListener(textWatcher)
            dialogView.lesseeAddress.addTextChangedListener(textWatcher)
            dialogView.lesseePostalCode.addTextChangedListener(textWatcher)
            dialogView.lesseeTIN.addTextChangedListener(textWatcher)
            dialogView.lesseeRent.addTextChangedListener(textWatcher)
            val from = LocalDate.now().with(TemporalAdjusters.firstDayOfNextMonth())
            val until = from.plusYears(1).minusMonths(1).with(TemporalAdjusters.lastDayOfMonth())
            dialogView.lesseeName.setText("")
            dialogView.lesseeAddress.setText("")
            dialogView.lesseePostalCode.setText("")
            dialogView.lesseeTIN.setText("")
            dialogView.lesseeRent.setText("")
            dialogView.lesseeFrom.setText(from.toShowDateFormat())
            dialogView.lesseeUntil.setText(until.toShowDateFormat())
            dialogView.deleteLessee.visibility = View.GONE
            dialogView.saveLessee.setText(R.string.save)
            dialogView.lesseeName.requestFocus()
        } else {
            dialogView.lesseeName.setText(lessee.name)
            dialogView.lesseeAddress.setText(lessee.address)
            dialogView.lesseePostalCode.setText(lessee.postalCode)
            dialogView.lesseeTIN.setText(lessee.tin)
            dialogView.lesseeRent.setText(lessee.rent.toString())
            dialogView.lesseeFrom.setText(LocalDate.parse(lessee.from).toShowDateFormat())
            dialogView.lesseeUntil.setText(LocalDate.parse(lessee.until).toShowDateFormat())
            dialogView.lesseeName.clearFocus()
            dialogView.lesseeAddress.clearFocus()
            dialogView.lesseePostalCode.clearFocus()
            dialogView.lesseeTIN.clearFocus()
            dialogView.lesseeRent.clearFocus()
            dialogView.lesseeFrom.clearFocus()
            dialogView.lesseeUntil.clearFocus()
            dialogView.deleteLessee.visibility = View.VISIBLE
            dialogView.saveLessee.setText(R.string.update)
            dialogView.lesseeName.addTextChangedListener(textWatcher)
            dialogView.lesseeAddress.addTextChangedListener(textWatcher)
            dialogView.lesseePostalCode.addTextChangedListener(textWatcher)
            dialogView.lesseeTIN.addTextChangedListener(textWatcher)
            dialogView.lesseeRent.addTextChangedListener(textWatcher)
        }

        dialog.show()
    }
}