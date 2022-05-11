package com.panosdim.flatman.ui.fragments

import android.os.Bundle
import android.text.InputFilter
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.panosdim.flatman.R
import com.panosdim.flatman.api.data.Resource
import com.panosdim.flatman.model.Balance
import com.panosdim.flatman.model.Flat
import com.panosdim.flatman.ui.adapters.BalanceAdapter
import com.panosdim.flatman.utils.*
import com.panosdim.flatman.viewmodel.BalanceViewModel
import com.panosdim.flatman.viewmodel.FlatViewModel
import com.panosdim.flatman.viewmodel.LesseeViewModel
import kotlinx.android.synthetic.main.dialog_balance.view.*
import kotlinx.android.synthetic.main.fragment_balance.*
import kotlinx.android.synthetic.main.fragment_balance.view.*
import java.time.LocalDate


class BalanceFragment : Fragment() {
    private val flatViewModel: FlatViewModel by viewModels()
    private val lesseeViewModel: LesseeViewModel by viewModels()
    private lateinit var dialog: BottomSheetDialog
    private lateinit var dialogView: View
    private lateinit var rootView: View
    private var balance: Balance? = null
    private var selectedFlat: Flat? = null
    private lateinit var flatSelectAdapter: ArrayAdapter<Flat>
    private val viewModel: BalanceViewModel by viewModels()
    private val textWatcher = generateTextWatcher(::validateForm)

    private fun balanceItemClicked(balanceItem: Balance) {
        balance = balanceItem
        showForm(balance)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getAllBalance().observe(viewLifecycleOwner) { resource ->
            if (resource != null) {
                when (resource) {
                    is Resource.Success -> {
                        progress_bar.visibility = View.GONE
                        rvBalance.visibility = View.VISIBLE
                        updateBalanceAdapter()
                        viewModel.getAllBalance().removeObservers(viewLifecycleOwner)
                    }
                    is Resource.Error -> {
                        Toast.makeText(
                            requireContext(),
                            resource.message,
                            Toast.LENGTH_LONG
                        ).show()
                        progress_bar.visibility = View.GONE
                        rvBalance.visibility = View.VISIBLE
                    }
                    is Resource.Loading -> {
                        progress_bar.visibility = View.VISIBLE
                        rvBalance.visibility = View.GONE
                    }
                }
            }
        }

        viewModel.balance.observe(viewLifecycleOwner) {
            updateBalanceAdapter()
        }

        swipe_refresh.setOnRefreshListener {
            viewModel.refreshBalance()
            swipe_refresh.isRefreshing = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_balance, container, false)
        dialog = BottomSheetDialog(requireContext())
        dialogView = inflater.inflate(R.layout.dialog_balance, container, false)
        dialog.setContentView(dialogView)

        val rvBalance = rootView.rvBalance
        rvBalance.setHasFixedSize(true)
        rvBalance.layoutManager = LinearLayoutManager(rootView.context)

        rootView.addNewBalance.setOnClickListener {
            balance = null
            showForm(balance)
        }

        dialogView.balanceComment.setOnEditorActionListener { _, actionId, event ->
            if (isFormValid() && (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE)) {
                balance?.let {
                    updateBalance(it)
                } ?: kotlin.run {
                    saveBalance()
                }
            }
            false
        }

        dialogView.balanceAmount.filters = arrayOf<InputFilter>(
            DecimalDigitsInputFilter(
                9,
                2
            )
        )

        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.list_item,
            resources.getStringArray(R.array.comments)
        )
        dialogView.balanceComment.setAdapter(adapter)
        @Suppress("DEPRECATION")
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        dialogView.balanceDate.setOnClickListener {
            val date: LocalDate = dialogView.balanceDate.text.toString().toLocalDate()

            //Date Picker
            val builder = MaterialDatePicker.Builder.datePicker()
            val constraintsBuilder = CalendarConstraints.Builder()
            constraintsBuilder.setOpenAt(date.toEpochMilli())
            builder.setCalendarConstraints(constraintsBuilder.build())
            builder.setTheme(R.style.ThemeOverlay_Catalog_MaterialCalendar_Custom)
            builder.setSelection(date.toEpochMilli())
            builder.setTitleText("Select Date")

            val picker: MaterialDatePicker<Long> = builder.build()
            picker.addOnPositiveButtonClickListener { selection ->
                dialogView.balanceDate.setText(fromEpochMilli(selection).toShowDateFormat())
            }

            picker.show(childFragmentManager, picker.toString())
        }

        dialogView.saveBalance.setOnClickListener {
            dialogView.prgIndicator.visibility = View.VISIBLE
            dialogView.saveBalance.isEnabled = false
            dialogView.deleteBalance.isEnabled = false

            balance?.let {
                updateBalance(it)
            } ?: kotlin.run {
                saveBalance()
            }
        }

        dialogView.deleteBalance.setOnClickListener {
            deleteBalance()
        }

        flatViewModel.flats.observe(viewLifecycleOwner) {
            if (it != null && it.isNotEmpty()) {
                rootView.addNewBalance.isEnabled = true
                flatSelectAdapter =
                    ArrayAdapter(requireContext(), R.layout.list_item, it)
                rootView.selectedFlat.setAdapter(flatSelectAdapter)
                selectedFlat = flatSelectAdapter.getItem(0)
                rootView.selectedFlat.setText(selectedFlat.toString(), false)

                updateBalanceAdapter()
            } else {
                rootView.addNewBalance.isEnabled = false
                rootView.rvBalance.adapter =
                    BalanceAdapter(mutableListOf()) { balanceItem: Balance ->
                        balanceItemClicked(
                            balanceItem
                        )
                    }
            }
        }

        rootView.selectedFlat.setOnItemClickListener { _, _, position, _ ->
            selectedFlat = flatSelectAdapter.getItem(position)
            updateBalanceAdapter()
        }

        return rootView
    }

    private fun updateBalanceAdapter() {
        val data = viewModel.balance.value
        if (data != null) {
            val temp = data.filter {
                it.flatId == (selectedFlat?.id ?: -1)
            }.sortedByDescending { it.date }
            rootView.rvBalance.adapter =
                BalanceAdapter(temp) { balanceItem: Balance -> balanceItemClicked(balanceItem) }
            (rootView.rvBalance.adapter as BalanceAdapter).notifyDataSetChanged()
        } else {
            rootView.rvBalance.adapter =
                BalanceAdapter(mutableListOf()) { balanceItem: Balance ->
                    balanceItemClicked(
                        balanceItem
                    )
                }
            (rootView.rvBalance.adapter as BalanceAdapter).notifyDataSetChanged()
        }

    }

    private fun deleteBalance() {
        balance?.let {
            viewModel.removeBalance(it).observe(viewLifecycleOwner) { resource ->
                if (resource != null) {
                    when (resource) {
                        is Resource.Success -> {
                            dialog.hide()
                            dialogView.prgIndicator.visibility = View.GONE
                            dialogView.deleteBalance.isEnabled = true
                            dialogView.saveBalance.isEnabled = true
                        }
                        is Resource.Error -> {
                            Toast.makeText(
                                requireContext(),
                                resource.message,
                                Toast.LENGTH_LONG
                            ).show()
                            dialogView.prgIndicator.visibility = View.GONE
                            dialogView.deleteBalance.isEnabled = true
                            dialogView.saveBalance.isEnabled = true
                        }
                        is Resource.Loading -> {
                            dialogView.prgIndicator.visibility = View.VISIBLE
                            dialogView.deleteBalance.isEnabled = false
                            dialogView.saveBalance.isEnabled = false
                        }
                    }
                }
            }
        }
    }

    private fun saveBalance() {
        val newBalance = Balance(
            null,
            dialogView.balanceDate.text.toString().toSQLDateFormat(),
            dialogView.balanceComment.text.toString(),
            selectedFlat?.id!!,
            dialogView.balanceAmount.text.toString().toFloat()
        )

        viewModel.addBalance(newBalance).observe(viewLifecycleOwner) { resource ->
            if (resource != null) {
                when (resource) {
                    is Resource.Success -> {
                        dialog.hide()
                        dialogView.prgIndicator.visibility = View.GONE
                        dialogView.deleteBalance.isEnabled = true
                        dialogView.saveBalance.isEnabled = true
                    }
                    is Resource.Error -> {
                        Toast.makeText(
                            requireContext(),
                            resource.message,
                            Toast.LENGTH_LONG
                        ).show()
                        dialogView.prgIndicator.visibility = View.GONE
                        dialogView.deleteBalance.isEnabled = true
                        dialogView.saveBalance.isEnabled = true
                    }
                    is Resource.Loading -> {
                        dialogView.prgIndicator.visibility = View.VISIBLE
                        dialogView.deleteBalance.isEnabled = false
                        dialogView.saveBalance.isEnabled = false
                    }
                }
            }
        }
    }

    private fun updateBalance(balance: Balance) {
        // Check if we change something in the object
        if (balance.date == dialogView.balanceDate.text.toString().toSQLDateFormat() &&
            balance.amount == dialogView.balanceAmount.text.toString().toFloat() &&
            balance.comment == dialogView.balanceComment.text.toString()
        ) {
            dialog.hide()
        } else {
            // Update Balance
            balance.date = dialogView.balanceDate.text.toString().toSQLDateFormat()
            balance.amount = dialogView.balanceAmount.text.toString().toFloat()
            balance.comment = dialogView.balanceComment.text.toString()

            viewModel.updateBalance(balance).observe(viewLifecycleOwner) { resource ->
                if (resource != null) {
                    when (resource) {
                        is Resource.Success -> {
                            dialog.hide()
                            dialogView.prgIndicator.visibility = View.GONE
                            dialogView.deleteBalance.isEnabled = true
                            dialogView.saveBalance.isEnabled = true
                        }
                        is Resource.Error -> {
                            Toast.makeText(
                                requireContext(),
                                resource.message,
                                Toast.LENGTH_LONG
                            ).show()
                            dialogView.prgIndicator.visibility = View.GONE
                            dialogView.deleteBalance.isEnabled = true
                            dialogView.saveBalance.isEnabled = true
                        }
                        is Resource.Loading -> {
                            dialogView.prgIndicator.visibility = View.VISIBLE
                            dialogView.deleteBalance.isEnabled = false
                            dialogView.saveBalance.isEnabled = false
                        }
                    }
                }
            }
        }
    }

    private fun validateForm() {
        val balanceDate = dialogView.balanceDate
        val balanceAmount = dialogView.balanceAmount
        val balanceComment = dialogView.balanceComment
        val saveBalance = dialogView.saveBalance

        saveBalance.isEnabled = true
        balanceDate.error = null
        balanceAmount.error = null
        balanceComment.error = null

        // Store values.
        val date = balanceDate.text.toString()
        val amount = balanceAmount.text.toString()
        val comment = balanceComment.text.toString()

        if (date.isEmpty()) {
            balanceDate.error = getString(R.string.error_field_required)
            saveBalance.isEnabled = false
        }

        if (amount.isEmpty()) {
            balanceAmount.error = getString(R.string.error_field_required)
            saveBalance.isEnabled = false
        }

        if (comment.isEmpty()) {
            balanceComment.error = getString(R.string.error_field_required)
            saveBalance.isEnabled = false
        }
    }

    private fun isFormValid(): Boolean {
        return dialogView.balanceDate.error == null &&
                dialogView.balanceAmount.error == null &&
                dialogView.balanceComment.error == null
    }

    private fun showForm(balance: Balance?) {
        dialogView.prgIndicator.visibility = View.GONE
        dialogView.saveBalance.isEnabled = true
        dialogView.deleteBalance.isEnabled = true

        dialogView.balanceDate.removeTextChangedListener(textWatcher)
        dialogView.balanceAmount.removeTextChangedListener(textWatcher)
        dialogView.balanceComment.removeTextChangedListener(textWatcher)
        dialogView.balanceDate.error = null
        dialogView.balanceAmount.error = null
        dialogView.balanceComment.error = null

        if (balance == null) {
            dialogView.balanceDate.addTextChangedListener(textWatcher)
            dialogView.balanceAmount.addTextChangedListener(textWatcher)
            dialogView.balanceComment.addTextChangedListener(textWatcher)
            val today = LocalDate.now()
            dialogView.balanceAmount.setText("")
            selectedFlat?.let {
                val rent = lesseeViewModel.getMonthlyRent(it)
                if (rent != null) {
                    dialogView.balanceAmount.setText(rent.toString())
                }
            }
            dialogView.balanceComment.setText("")
            dialogView.balanceDate.setText(today.toShowDateFormat())
            dialogView.deleteBalance.visibility = View.GONE
            dialogView.saveBalance.setText(R.string.save)
            dialogView.balanceAmount.requestFocus()
        } else {
            dialogView.balanceDate.setText(LocalDate.parse(balance.date).toShowDateFormat())
            dialogView.balanceAmount.setText(balance.amount.toString())
            dialogView.balanceComment.setText(balance.comment)
            dialogView.balanceDate.clearFocus()
            dialogView.balanceAmount.clearFocus()
            dialogView.balanceComment.clearFocus()
            dialogView.deleteBalance.visibility = View.VISIBLE
            dialogView.saveBalance.setText(R.string.update)
            dialogView.balanceDate.addTextChangedListener(textWatcher)
            dialogView.balanceAmount.addTextChangedListener(textWatcher)
            dialogView.balanceComment.addTextChangedListener(textWatcher)
        }

        dialog.show()
    }
}