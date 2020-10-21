package com.panosdim.flatman.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.panosdim.flatman.R
import com.panosdim.flatman.balanceList
import com.panosdim.flatman.flatsList
import com.panosdim.flatman.model.Balance
import com.panosdim.flatman.model.Flat
import com.panosdim.flatman.repository
import com.panosdim.flatman.ui.adapters.BalanceAdapter
import com.panosdim.flatman.utils.*
import kotlinx.android.synthetic.main.dialog_balance.view.*
import kotlinx.android.synthetic.main.fragment_balance.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate


class BalanceFragment : Fragment() {

    private lateinit var dialog: BottomSheetDialog
    private lateinit var dialogView: View
    private lateinit var rootView: View
    private var balance: Balance? = null
    private var selectedFlat: Flat? = null
    private lateinit var flatSelectAdapter: ArrayAdapter<Flat>
    private val scope = CoroutineScope(Dispatchers.Main)
    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            validateForm()
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            // Not Needed
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            // Not Needed
        }
    }

    private fun balanceItemClicked(balanceItem: Balance) {
        balance = balanceItem
        showForm(balance)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
                saveBalance()
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

        flatsList.observe(viewLifecycleOwner, {
            if (it != null && it.size > 0) {
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
        })

        rootView.selectedFlat.setOnItemClickListener { _, _, position, _ ->
            selectedFlat = flatSelectAdapter.getItem(position)
            updateBalanceAdapter()
        }

        balanceList.observe(viewLifecycleOwner, {
            updateBalanceAdapter()
        })

        return rootView
    }

    private fun updateBalanceAdapter() {
        val temp = balanceList.value!!.filter {
            it.flatId == selectedFlat!!.id
        }.sortedByDescending { it.date }
        rootView.rvBalance.adapter =
            BalanceAdapter(temp) { balanceItem: Balance -> balanceItemClicked(balanceItem) }
        (rootView.rvBalance.adapter as BalanceAdapter).notifyDataSetChanged()
    }

    private fun deleteBalance() {
        balance?.let {
            scope.launch {
                dialogView.prgIndicator.visibility = View.VISIBLE
                dialogView.deleteBalance.isEnabled = false
                dialogView.saveBalance.isEnabled = false
                try {
                    withContext(Dispatchers.IO) {
                        val response = repository.deleteBalance(it.id!!)
                        when (response.code()) {
                            204 -> {
                                val temp = balanceList.value
                                temp!!.remove(it)
                                balanceList.postValue(temp)
                            }
                            404 -> {
                                Toast.makeText(
                                    requireContext(),
                                    "Error deleting balance. Balance not found.",
                                    Toast.LENGTH_LONG
                                ).show()
                                dialogView.prgIndicator.visibility = View.GONE
                                dialogView.deleteBalance.isEnabled = true
                                dialogView.saveBalance.isEnabled = true
                            }
                            else -> {
                                Toast.makeText(
                                    requireContext(),
                                    "Error deleting balance. Unknown Error.",
                                    Toast.LENGTH_LONG
                                ).show()
                                dialogView.prgIndicator.visibility = View.GONE
                                dialogView.deleteBalance.isEnabled = true
                                dialogView.saveBalance.isEnabled = true
                            }
                        }
                    }
                    dialog.hide()
                } catch (ex: HttpException) {
                    Toast.makeText(
                        requireContext(),
                        "Error deleting balance.",
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
                    dialogView.saveBalance.isEnabled = true
                    dialogView.deleteBalance.isEnabled = true
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

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val response = repository.createNewBalance(newBalance)
                    val temp = balanceList.value
                    temp?.add(response)
                    balanceList.postValue(temp)
                }
                dialog.hide()
            } catch (e: HttpException) {
                Toast.makeText(requireContext(), "Error saving balance.", Toast.LENGTH_SHORT)
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
                dialogView.saveBalance.isEnabled = true
                dialogView.deleteBalance.isEnabled = true
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

            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val response = repository.updateBalance(balance.id!!, balance)
                        val temp = balanceList.value
                        val index = temp!!.indexOfFirst { (id) -> id == response.id }
                        temp[index] = response
                        balanceList.postValue(temp)
                    }
                    dialog.hide()
                } catch (e: HttpException) {
                    Toast.makeText(
                        requireContext(),
                        "Error saving balance.",
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
                    dialogView.saveBalance.isEnabled = true
                    dialogView.deleteBalance.isEnabled = true
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