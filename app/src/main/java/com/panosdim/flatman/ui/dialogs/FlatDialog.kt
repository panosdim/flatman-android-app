package com.panosdim.flatman.ui.dialogs

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.panosdim.flatman.R
import com.panosdim.flatman.api.data.Resource
import com.panosdim.flatman.databinding.DialogFlatBinding
import com.panosdim.flatman.model.Flat
import com.panosdim.flatman.utils.generateTextWatcher
import com.panosdim.flatman.viewmodel.FlatViewModel

class FlatDialog : BottomSheetDialogFragment() {
    private var _binding: DialogFlatBinding? = null
    private val binding get() = _binding!!
    private var flat: Flat? = null
    private val textWatcher = generateTextWatcher(::validateForm)
    private val viewModel: FlatViewModel by viewModels(ownerProducer = { requireParentFragment() })


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFlatBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.flatFloor.setOnEditorActionListener { _, actionId, event ->
            if (isFormValid() && (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE)) {
                flat?.let {
                    updateFlat(it)
                } ?: kotlin.run {
                    saveFlat()
                }
            }
            false
        }

        binding.saveFlat.setOnClickListener {
            flat?.let {
                updateFlat(it)
            } ?: kotlin.run {
                saveFlat()
            }
        }

        binding.deleteFlat.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(resources.getString(R.string.delete_flat_title))
                .setMessage(resources.getString(R.string.delete_flat_supporting_text))
                .setNegativeButton(resources.getString(R.string.decline)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(resources.getString(R.string.accept)) { dialog, _ ->
                    dialog.dismiss()
                    deleteFlat()
                }
                .show()
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPause() {
        super.onPause()
        flat = null
    }

    private fun deleteFlat() {
        flat?.let {
            viewModel.removeFlat(it).observe(viewLifecycleOwner) { resource ->
                if (resource != null) {
                    when (resource) {
                        is Resource.Success -> {
                            dismiss()
                            binding.prgIndicator.visibility = View.GONE
                            binding.deleteFlat.isEnabled = true
                            binding.saveFlat.isEnabled = true
                        }
                        is Resource.Error -> {
                            Toast.makeText(
                                requireContext(),
                                resource.message,
                                Toast.LENGTH_LONG
                            ).show()
                            binding.prgIndicator.visibility = View.GONE
                            binding.deleteFlat.isEnabled = true
                            binding.saveFlat.isEnabled = true
                        }
                        is Resource.Loading -> {
                            binding.prgIndicator.visibility = View.VISIBLE
                            binding.deleteFlat.isEnabled = false
                            binding.saveFlat.isEnabled = false
                        }
                    }
                }
            }
        }
    }

    private fun saveFlat() {
        val newFlat = Flat(
            null,
            binding.flatName.text.toString(),
            binding.flatAddress.text.toString(),
            binding.flatFloor.text.toString().toInt()
        )

        viewModel.addFlat(newFlat).observe(viewLifecycleOwner) { resource ->
            if (resource != null) {
                when (resource) {
                    is Resource.Success -> {
                        dismiss()
                        binding.prgIndicator.visibility = View.GONE
                        binding.deleteFlat.isEnabled = true
                        binding.saveFlat.isEnabled = true
                    }
                    is Resource.Error -> {
                        Toast.makeText(
                            requireContext(),
                            resource.message,
                            Toast.LENGTH_LONG
                        ).show()
                        binding.prgIndicator.visibility = View.GONE
                        binding.deleteFlat.isEnabled = true
                        binding.saveFlat.isEnabled = true
                    }
                    is Resource.Loading -> {
                        binding.prgIndicator.visibility = View.VISIBLE
                        binding.deleteFlat.isEnabled = false
                        binding.saveFlat.isEnabled = false
                    }
                }
            }
        }
    }

    private fun updateFlat(flat: Flat) {
        // Check if we change something in the object
        if (flat.name == binding.flatName.text.toString() &&
            flat.address == binding.flatAddress.text.toString() &&
            flat.floor == binding.flatFloor.text.toString().toInt()
        ) {
            dismiss()
        } else {
            // Update Flat
            flat.name = binding.flatName.text.toString()
            flat.address = binding.flatAddress.text.toString()
            flat.floor = binding.flatFloor.text.toString().toInt()

            viewModel.updateFlat(flat).observe(viewLifecycleOwner) { resource ->
                if (resource != null) {
                    when (resource) {
                        is Resource.Success -> {
                            dismiss()
                            binding.prgIndicator.visibility = View.GONE
                            binding.deleteFlat.isEnabled = true
                            binding.saveFlat.isEnabled = true
                        }
                        is Resource.Error -> {
                            Toast.makeText(
                                requireContext(),
                                resource.message,
                                Toast.LENGTH_LONG
                            ).show()
                            binding.prgIndicator.visibility = View.GONE
                            binding.deleteFlat.isEnabled = true
                            binding.saveFlat.isEnabled = true
                        }
                        is Resource.Loading -> {
                            binding.prgIndicator.visibility = View.VISIBLE
                            binding.deleteFlat.isEnabled = false
                            binding.saveFlat.isEnabled = false
                        }
                    }
                }
            }
        }
    }

    fun showForm(flatItm: Flat?) {
        binding.prgIndicator.visibility = View.GONE
        binding.saveFlat.isEnabled = true
        binding.deleteFlat.isEnabled = true

        binding.flatName.removeTextChangedListener(textWatcher)
        binding.flatAddress.removeTextChangedListener(textWatcher)
        binding.flatFloor.removeTextChangedListener(textWatcher)
        binding.flatName.error = null
        binding.flatAddress.error = null
        binding.flatFloor.error = null

        flat = flatItm

        if (flatItm == null) {
            binding.flatName.addTextChangedListener(textWatcher)
            binding.flatAddress.addTextChangedListener(textWatcher)
            binding.flatFloor.addTextChangedListener(textWatcher)
            binding.flatName.setText("")
            binding.flatAddress.setText("")
            binding.flatFloor.setText("")
            binding.deleteFlat.visibility = View.GONE
            binding.saveFlat.setText(R.string.save)
            binding.flatName.requestFocus()
        } else {
            binding.flatName.setText(flatItm.name)
            binding.flatAddress.setText(flatItm.address)
            binding.flatFloor.setText(flatItm.floor.toString())
            binding.flatName.clearFocus()
            binding.flatAddress.clearFocus()
            binding.flatFloor.clearFocus()
            binding.deleteFlat.visibility = View.VISIBLE
            binding.saveFlat.setText(R.string.update)
            binding.flatName.addTextChangedListener(textWatcher)
            binding.flatAddress.addTextChangedListener(textWatcher)
            binding.flatFloor.addTextChangedListener(textWatcher)
        }
    }

    private fun validateForm() {
        val flatName = binding.flatName
        val flatAddress = binding.flatAddress
        val flatFloor = binding.flatFloor
        val saveFlat = binding.saveFlat
        saveFlat.isEnabled = true
        flatName.error = null
        flatAddress.error = null
        flatFloor.error = null

        // Store values.
        val name = flatName.text.toString()
        val address = flatAddress.text.toString()
        val floor = flatFloor.text.toString()

        if (name.isEmpty()) {
            flatName.error = getString(R.string.error_field_required)
            saveFlat.isEnabled = false
        }

        if (address.isEmpty()) {
            flatAddress.error = getString(R.string.error_field_required)
            saveFlat.isEnabled = false
        }

        if (floor.isEmpty()) {
            flatFloor.error = getString(R.string.error_field_required)
            saveFlat.isEnabled = false
        }
    }

    private fun isFormValid(): Boolean {
        return binding.flatName.error == null &&
                binding.flatAddress.error == null &&
                binding.flatFloor.error == null
    }

    companion object {
        const val TAG = "FlatDialog"
    }
}