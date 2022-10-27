package com.panosdim.flatman.ui.dialogs

import android.os.Bundle
import android.text.InputFilter
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.panosdim.flatman.R
import com.panosdim.flatman.api.data.Resource
import com.panosdim.flatman.databinding.DialogBalanceBinding
import com.panosdim.flatman.model.Balance
import com.panosdim.flatman.model.Flat
import com.panosdim.flatman.utils.*
import com.panosdim.flatman.viewmodel.BalanceViewModel
import com.panosdim.flatman.viewmodel.LesseeViewModel
import java.time.LocalDate

class BalanceDialog : BottomSheetDialogFragment() {
    private var _binding: DialogBalanceBinding? = null
    private val binding get() = _binding!!
    private var balance: Balance? = null
    private var selectedFlat: Flat? = null
    private val textWatcher = generateTextWatcher(::validateForm)
    private val viewModel: BalanceViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private val lesseeViewModel: LesseeViewModel by viewModels(ownerProducer = { requireParentFragment() })
    var flats: List<Flat> = mutableListOf()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogBalanceBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.balanceComment.setOnEditorActionListener { _, actionId, event ->
            if (isFormValid() && (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE)) {
                balance?.let {
                    updateBalance(it)
                } ?: kotlin.run {
                    saveBalance()
                }
            }
            false
        }

        binding.balanceAmount.filters = arrayOf<InputFilter>(
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
        binding.balanceComment.setAdapter(adapter)

        binding.balanceDate.setOnClickListener {
            val date: LocalDate = binding.balanceDate.text.toString().toLocalDate()

            //Date Picker
            val builder = MaterialDatePicker.Builder.datePicker()
            val constraintsBuilder = CalendarConstraints.Builder()
            constraintsBuilder.setOpenAt(date.toEpochMilli())
            builder.setCalendarConstraints(constraintsBuilder.build())
            builder.setSelection(date.toEpochMilli())
            builder.setTitleText("Select Date")

            val picker: MaterialDatePicker<Long> = builder.build()
            picker.addOnPositiveButtonClickListener { selection ->
                binding.balanceDate.setText(fromEpochMilli(selection).toShowDateFormat())
            }

            picker.show(childFragmentManager, picker.toString())
        }

        binding.saveBalance.setOnClickListener {
            binding.prgIndicator.visibility = View.VISIBLE
            binding.saveBalance.isEnabled = false
            binding.deleteBalance.isEnabled = false

            balance?.let {
                updateBalance(it)
            } ?: kotlin.run {
                saveBalance()
            }
        }

        binding.deleteBalance.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(resources.getString(R.string.delete_balance_title))
                .setMessage(resources.getString(R.string.delete_balance_supporting_text))
                .setNegativeButton(resources.getString(R.string.decline)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(resources.getString(R.string.accept)) { dialog, _ ->
                    dialog.dismiss()
                    deleteBalance()
                }
                .show()
        }

        binding.flat.setOnItemClickListener { parent, _, position, _ ->
            selectedFlat = (parent.getItemAtPosition(position) as Flat)
            selectedFlat?.let {
                val rent = lesseeViewModel.getMonthlyRent(it)
                if (rent != null) {
                    binding.balanceAmount.setText(rent.toString())
                }
            }
            validateForm()
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPause() {
        super.onPause()
        balance = null
        // Workaround as there is a bug in AutoCompleteView filter setting
        binding.flat.setText("", false)
        selectedFlat = null
    }

    private fun deleteBalance() {
        balance?.let {
            viewModel.removeBalance(it).observe(viewLifecycleOwner) { resource ->
                if (resource != null) {
                    when (resource) {
                        is Resource.Success -> {
                            dismiss()
                            binding.prgIndicator.visibility = View.GONE
                            binding.deleteBalance.isEnabled = true
                            binding.saveBalance.isEnabled = true
                        }
                        is Resource.Error -> {
                            Toast.makeText(
                                requireContext(),
                                resource.message,
                                Toast.LENGTH_LONG
                            ).show()
                            binding.prgIndicator.visibility = View.GONE
                            binding.deleteBalance.isEnabled = true
                            binding.saveBalance.isEnabled = true
                        }
                        is Resource.Loading -> {
                            binding.prgIndicator.visibility = View.VISIBLE
                            binding.deleteBalance.isEnabled = false
                            binding.saveBalance.isEnabled = false
                        }
                    }
                }
            }
        }
    }

    private fun saveBalance() {
        val newBalance = Balance(
            null,
            binding.balanceDate.text.toString().toSQLDateFormat(),
            binding.balanceComment.text.toString(),
            selectedFlat?.id!!,
            binding.balanceAmount.text.toString().toFloat()
        )

        viewModel.addBalance(newBalance).observe(viewLifecycleOwner) { resource ->
            if (resource != null) {
                when (resource) {
                    is Resource.Success -> {
                        dismiss()
                        binding.prgIndicator.visibility = View.GONE
                        binding.deleteBalance.isEnabled = true
                        binding.saveBalance.isEnabled = true
                    }
                    is Resource.Error -> {
                        Toast.makeText(
                            requireContext(),
                            resource.message,
                            Toast.LENGTH_LONG
                        ).show()
                        binding.prgIndicator.visibility = View.GONE
                        binding.deleteBalance.isEnabled = true
                        binding.saveBalance.isEnabled = true
                    }
                    is Resource.Loading -> {
                        binding.prgIndicator.visibility = View.VISIBLE
                        binding.deleteBalance.isEnabled = false
                        binding.saveBalance.isEnabled = false
                    }
                }
            }
        }
    }

    private fun updateBalance(balance: Balance) {
        // Check if we change something in the object
        if (balance.date == binding.balanceDate.text.toString().toSQLDateFormat() &&
            balance.amount == binding.balanceAmount.text.toString().toFloat() &&
            balance.comment == binding.balanceComment.text.toString() &&
            balance.flatId == selectedFlat?.id!!
        ) {
            dismiss()
        } else {
            // Update Balance
            val updatedBalance = balance.copy(
                date = binding.balanceDate.text.toString().toSQLDateFormat(),
                amount = binding.balanceAmount.text.toString().toFloat(),
                comment = binding.balanceComment.text.toString(),
                flatId = selectedFlat?.id!!,
            )

            viewModel.updateBalance(updatedBalance).observe(viewLifecycleOwner) { resource ->
                if (resource != null) {
                    when (resource) {
                        is Resource.Success -> {
                            dismiss()
                            binding.prgIndicator.visibility = View.GONE
                            binding.deleteBalance.isEnabled = true
                            binding.saveBalance.isEnabled = true
                        }
                        is Resource.Error -> {
                            Toast.makeText(
                                requireContext(),
                                resource.message,
                                Toast.LENGTH_LONG
                            ).show()
                            binding.prgIndicator.visibility = View.GONE
                            binding.deleteBalance.isEnabled = true
                            binding.saveBalance.isEnabled = true
                        }
                        is Resource.Loading -> {
                            binding.prgIndicator.visibility = View.VISIBLE
                            binding.deleteBalance.isEnabled = false
                            binding.saveBalance.isEnabled = false
                        }
                    }
                }
            }
        }
    }

    private fun validateForm() {
        val balanceDate = binding.balanceDate
        val balanceAmount = binding.balanceAmount
        val balanceComment = binding.balanceComment
        val saveBalance = binding.saveBalance
        val flat = binding.flat

        saveBalance.isEnabled = true
        balanceDate.error = null
        balanceAmount.error = null
        balanceComment.error = null
        flat.error = null

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

        if (selectedFlat == null) {
            flat.error = getString(R.string.error_field_required)
            saveBalance.isEnabled = false
        }
    }

    private fun isFormValid(): Boolean {
        return binding.balanceDate.error == null &&
                binding.balanceAmount.error == null &&
                binding.balanceComment.error == null &&
                binding.flat.error == null
    }

    fun showForm(balanceItm: Balance?) {
        val adapter = ArrayAdapter(requireContext(), R.layout.list_item, flats)
        binding.flat.setAdapter(adapter)

        binding.prgIndicator.visibility = View.GONE
        binding.saveBalance.isEnabled = true
        binding.deleteBalance.isEnabled = true

        binding.balanceDate.removeTextChangedListener(textWatcher)
        binding.balanceAmount.removeTextChangedListener(textWatcher)
        binding.balanceComment.removeTextChangedListener(textWatcher)
        binding.balanceDate.error = null
        binding.balanceAmount.error = null
        binding.balanceComment.error = null
        binding.flat.error = null

        balance = balanceItm
        if (balanceItm == null) {
            binding.balanceDate.addTextChangedListener(textWatcher)
            binding.balanceAmount.addTextChangedListener(textWatcher)
            binding.balanceComment.addTextChangedListener(textWatcher)
            val today = LocalDate.now()
            binding.balanceAmount.setText("")
            selectedFlat?.let {
                val rent = lesseeViewModel.getMonthlyRent(it)
                if (rent != null) {
                    binding.balanceAmount.setText(rent.toString())
                }
            }
            binding.balanceComment.setText("")
            binding.balanceDate.setText(today.toShowDateFormat())
            binding.deleteBalance.visibility = View.GONE
            binding.saveBalance.setText(R.string.save)
            binding.flat.requestFocus()
        } else {
            binding.balanceDate.setText(LocalDate.parse(balanceItm.date).toShowDateFormat())
            binding.balanceAmount.setText(balanceItm.amount.toString())
            binding.balanceComment.setText(balanceItm.comment)
            selectedFlat = flats.first { it.id == balanceItm.flatId }
            binding.flat.setText(selectedFlat!!.name, false)
            binding.balanceDate.clearFocus()
            binding.balanceAmount.clearFocus()
            binding.balanceComment.clearFocus()
            binding.deleteBalance.visibility = View.VISIBLE
            binding.saveBalance.setText(R.string.update)
            binding.balanceDate.addTextChangedListener(textWatcher)
            binding.balanceAmount.addTextChangedListener(textWatcher)
            binding.balanceComment.addTextChangedListener(textWatcher)
        }
    }

    companion object {
        const val TAG = "BalanceDialog"
    }
}