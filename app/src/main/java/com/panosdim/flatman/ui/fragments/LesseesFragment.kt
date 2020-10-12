package com.panosdim.flatman.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.panosdim.flatman.R
import com.panosdim.flatman.flatsList
import com.panosdim.flatman.lesseesList
import com.panosdim.flatman.model.Flat
import com.panosdim.flatman.model.Lessee
import com.panosdim.flatman.repository
import com.panosdim.flatman.rest.data.CheckTinResponse
import com.panosdim.flatman.ui.adapters.LesseesAdapter
import com.panosdim.flatman.ui.login.afterTextChanged
import com.panosdim.flatman.utils.*
import kotlinx.android.synthetic.main.dialog_lessee.view.*
import kotlinx.android.synthetic.main.fragment_lessees.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters


class LesseesFragment : Fragment() {
    private lateinit var dialog: BottomSheetDialog
    private lateinit var dialogView: View
    private lateinit var rootView: View
    private var lessee: Lessee? = null
    private var selectedFlat: Flat? = null
    private lateinit var flatSelectAdapter: ArrayAdapter<Flat>
    private val scope = CoroutineScope(Dispatchers.Main)
    private val postalCodeRegex = """^[12345678][0-9]{4}$""".toRegex()

    private fun lesseeItemClicked(lesseeItem: Lessee) {
        lessee = lesseeItem
        showForm(lessee)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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

        dialogView.lesseeName.afterTextChanged {
            validateForm()
        }

        dialogView.lesseeAddress.afterTextChanged {
            validateForm()
        }

        dialogView.lesseePostalCode.afterTextChanged {
            validateForm()
        }

        dialogView.lesseeTIN.afterTextChanged {
            validateForm()
        }

        dialogView.lesseeRent.afterTextChanged {
            validateForm()
        }

        dialogView.lesseeFrom.afterTextChanged {
            validateForm()
        }

        dialogView.lesseeUntil.afterTextChanged {
            validateForm()
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
            builder.setTheme(R.style.ThemeOverlay_Catalog_MaterialCalendar_Custom)
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
            builder.setTheme(R.style.ThemeOverlay_Catalog_MaterialCalendar_Custom)
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

        flatsList.observe(viewLifecycleOwner, {
            if (it != null && it.size > 0) {
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
        })

        rootView.selectedFlat.setOnItemClickListener { _, _, position, _ ->
            selectedFlat = flatSelectAdapter.getItem(position)
            updateLesseeAdapter()
        }

        return rootView
    }

    private fun updateLesseeAdapter() {
        val temp = lesseesList.filter {
            it.flatId == selectedFlat!!.id
        }
        rootView.rvLessees.adapter =
            LesseesAdapter(temp) { lesseeItem: Lessee -> lesseeItemClicked(lesseeItem) }
        (rootView.rvLessees.adapter as LesseesAdapter).notifyDataSetChanged()
    }

    private fun deleteLessee() {
        lessee?.let {
            scope.launch {
                dialogView.prgIndicator.visibility = View.VISIBLE
                dialogView.deleteLessee.isEnabled = false
                dialogView.saveLessee.isEnabled = false
                try {
                    withContext(Dispatchers.IO) {
                        val response = repository.deleteLessee(it.id!!)
                        when (response.code()) {
                            204 -> {
                                lesseesList.remove(it)
                            }
                            404 -> {
                                Toast.makeText(
                                    requireContext(),
                                    "Error deleting lessee. Lessee not found.",
                                    Toast.LENGTH_LONG
                                ).show()
                                dialogView.prgIndicator.visibility = View.GONE
                                dialogView.deleteLessee.isEnabled = true
                                dialogView.saveLessee.isEnabled = true
                            }
                            else -> {
                                Toast.makeText(
                                    requireContext(),
                                    "Error deleting lessee. Unknown Error.",
                                    Toast.LENGTH_LONG
                                ).show()
                                dialogView.prgIndicator.visibility = View.GONE
                                dialogView.deleteLessee.isEnabled = true
                                dialogView.saveLessee.isEnabled = true
                            }
                        }
                    }
                    updateLesseeAdapter()
                    dialog.hide()
                } catch (ex: HttpException) {
                    Toast.makeText(
                        requireContext(),
                        "Error deleting lessee.",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (t: SocketTimeoutException) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.connection_timeout),
                        Toast.LENGTH_LONG
                    )
                        .show()
                } catch (d: UnknownHostException) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.unknown_host),
                        Toast.LENGTH_LONG
                    )
                        .show()
                } finally {
                    dialogView.prgIndicator.visibility = View.GONE
                    dialogView.saveLessee.isEnabled = true
                    dialogView.deleteLessee.isEnabled = true
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
            dialogView.lesseeTIN.text.toString().toInt()
        )

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val response = repository.createNewLessee(newLessee)
                    lesseesList.add(response)
                }
                updateLesseeAdapter()
                dialog.hide()
            } catch (e: HttpException) {
                Toast.makeText(requireContext(), "Error saving lessee.", Toast.LENGTH_SHORT)
                    .show()
            } catch (t: SocketTimeoutException) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.connection_timeout),
                    Toast.LENGTH_LONG
                )
                    .show()
            } catch (d: UnknownHostException) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.unknown_host),
                    Toast.LENGTH_LONG
                )
                    .show()
            } finally {
                dialogView.prgIndicator.visibility = View.GONE
                dialogView.saveLessee.isEnabled = true
                dialogView.deleteLessee.isEnabled = true
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
            lessee.tin == dialogView.lesseeTIN.text.toString().toInt()
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
            lessee.tin = dialogView.lesseeTIN.text.toString().toInt()

            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val response = repository.updateLessee(lessee.id!!, lessee)
                        val index = lesseesList.indexOfFirst { (id) -> id == response.id }
                        lesseesList[index] = response
                    }
                    updateLesseeAdapter()
                    dialog.hide()
                } catch (e: HttpException) {
                    Toast.makeText(
                        requireContext(),
                        "Error saving lessee.",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                } catch (t: SocketTimeoutException) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.connection_timeout),
                        Toast.LENGTH_LONG
                    )
                        .show()
                } catch (d: UnknownHostException) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.unknown_host),
                        Toast.LENGTH_LONG
                    )
                        .show()
                } finally {
                    dialogView.prgIndicator.visibility = View.GONE
                    dialogView.saveLessee.isEnabled = true
                    dialogView.deleteLessee.isEnabled = true
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
        val lesseeFrom = dialogView.lesseeFrom
        val lesseeUntil = dialogView.lesseeUntil
        val saveLessee = dialogView.saveLessee

        saveLessee.isEnabled = true
        lesseeName.error = null
        lesseeAddress.error = null
        lesseePostalCode.error = null
        lesseeTIN.error = null
        lesseeRent.error = null
        lesseeFrom.error = null
        lesseeUntil.error = null

        if (lesseeName.text!!.isEmpty()) {
            lesseeName.error = getString(R.string.error_field_required)
            saveLessee.isEnabled = false
        }

        if (lesseeAddress.text!!.isEmpty()) {
            lesseeAddress.error = getString(R.string.error_field_required)
            saveLessee.isEnabled = false
        }

        if (lesseePostalCode.text!!.isEmpty()) {
            lesseePostalCode.error = getString(R.string.error_field_required)
            saveLessee.isEnabled = false
        } else {
            if (!postalCodeRegex.matches(lesseePostalCode.text.toString())) {
                lesseePostalCode.error = getString(R.string.error_postal_code)
                saveLessee.isEnabled = false
            }
        }

        if (lesseeTIN.text!!.isEmpty()) {
            lesseeTIN.error = getString(R.string.error_field_required)
            saveLessee.isEnabled = false
        }

        if (lesseeTIN.text.toString().length == 9) {
            scope.launch {
                lateinit var response: CheckTinResponse
                withContext(Dispatchers.IO) {
                    response = repository.checkTin(lesseeTIN.text.toString())
                }
                if (!(response.validStructure && response.validSyntax)) {
                    lesseeTIN.error = getString(R.string.error_tin)
                    saveLessee.isEnabled = false
                }
            }
        }

        if (lesseeRent.text!!.isEmpty()) {
            lesseeRent.error = getString(R.string.error_field_required)
            saveLessee.isEnabled = false
        }
    }

    private fun showForm(lessee: Lessee?) {
        dialogView.prgIndicator.visibility = View.GONE
        dialogView.saveLessee.isEnabled = true
        dialogView.deleteLessee.isEnabled = true
        if (lessee == null) {
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
            dialogView.lesseeTIN.setText(lessee.tin.toString())
            dialogView.lesseeRent.setText(lessee.rent.toString())
            dialogView.lesseeFrom.setText(LocalDate.parse(lessee.from).toShowDateFormat())
            dialogView.lesseeUntil.setText(LocalDate.parse(lessee.until).toShowDateFormat())
            dialogView.deleteLessee.visibility = View.VISIBLE
            dialogView.saveLessee.setText(R.string.update)
        }

        dialog.show()
    }
}