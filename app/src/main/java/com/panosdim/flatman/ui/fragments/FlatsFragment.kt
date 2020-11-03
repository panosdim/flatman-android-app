package com.panosdim.flatman.ui.fragments

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.panosdim.flatman.R
import com.panosdim.flatman.api.data.Resource
import com.panosdim.flatman.model.Flat
import com.panosdim.flatman.ui.adapters.FlatsAdapter
import com.panosdim.flatman.utils.generateTextWatcher
import com.panosdim.flatman.viewmodel.FlatViewModel
import kotlinx.android.synthetic.main.dialog_flat.view.*
import kotlinx.android.synthetic.main.fragment_flats.*
import kotlinx.android.synthetic.main.fragment_flats.view.*


class FlatsFragment : Fragment() {
    private val flatsViewAdapter =
        FlatsAdapter(mutableListOf()) { flatItem: Flat -> flatItemClicked(flatItem) }
    private lateinit var dialog: BottomSheetDialog
    private lateinit var dialogView: View
    private var flat: Flat? = null
    private val viewModel: FlatViewModel by viewModels()
    private val textWatcher = generateTextWatcher(::validateForm)

    private fun flatItemClicked(flatItem: Flat) {
        flat = flatItem
        showForm(flat)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getAllFlats().observe(viewLifecycleOwner) { resource ->
            if (resource != null) {
                when (resource) {
                    is Resource.Success -> {
                        rvFlats.adapter =
                            resource.data?.let {
                                FlatsAdapter(it) { flatItem: Flat ->
                                    flatItemClicked(
                                        flatItem
                                    )
                                }
                            } ?: FlatsAdapter(mutableListOf()) { flatItem: Flat ->
                                flatItemClicked(
                                    flatItem
                                )
                            }

                        (rvFlats.adapter as FlatsAdapter).notifyDataSetChanged()
                        progress_bar.visibility = View.GONE
                        rvFlats.visibility = View.VISIBLE

                        viewModel.getAllFlats().removeObservers(viewLifecycleOwner)
                    }
                    is Resource.Error -> {
                        Toast.makeText(
                            requireContext(),
                            resource.message,
                            Toast.LENGTH_LONG
                        ).show()
                        progress_bar.visibility = View.GONE
                        rvFlats.visibility = View.VISIBLE
                    }
                    is Resource.Loading -> {
                        progress_bar.visibility = View.VISIBLE
                        rvFlats.visibility = View.GONE
                    }
                }
            }
        }

        viewModel.flats.observe(viewLifecycleOwner) {
            rvFlats.adapter =
                FlatsAdapter(it) { flatItem: Flat -> flatItemClicked(flatItem) }
            (rvFlats.adapter as FlatsAdapter).notifyDataSetChanged()
        }

        swipe_refresh.setOnRefreshListener {
            viewModel.refreshFlats()
            swipe_refresh.isRefreshing = false
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_flats, container, false)
        dialog = BottomSheetDialog(requireContext())
        dialogView = inflater.inflate(R.layout.dialog_flat, container, false)
        dialog.setContentView(dialogView)

        val rvFlats = root.rvFlats
        rvFlats.setHasFixedSize(true)
        rvFlats.layoutManager = LinearLayoutManager(root.context)
        rvFlats.adapter = flatsViewAdapter

        root.addNewFlat.setOnClickListener {
            flat = null
            showForm(flat)
        }

        dialogView.flatFloor.setOnEditorActionListener { _, actionId, event ->
            if (isFormValid() && (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE)) {
                flat?.let {
                    updateFlat(it)
                } ?: kotlin.run {
                    saveFlat()
                }
            }
            false
        }

        dialogView.saveFlat.setOnClickListener {
            flat?.let {
                updateFlat(it)
            } ?: kotlin.run {
                saveFlat()
            }
        }

        dialogView.deleteFlat.setOnClickListener {
            deleteFlat()
        }

        return root
    }

    private fun deleteFlat() {
        flat?.let {
            viewModel.removeFlat(it).observe(viewLifecycleOwner) { resource ->
                if (resource != null) {
                    when (resource) {
                        is Resource.Success -> {
                            dialog.hide()
                            dialogView.prgIndicator.visibility = View.GONE
                            dialogView.deleteFlat.isEnabled = true
                            dialogView.saveFlat.isEnabled = true
                        }
                        is Resource.Error -> {
                            Toast.makeText(
                                requireContext(),
                                resource.message,
                                Toast.LENGTH_LONG
                            ).show()
                            dialogView.prgIndicator.visibility = View.GONE
                            dialogView.deleteFlat.isEnabled = true
                            dialogView.saveFlat.isEnabled = true
                        }
                        is Resource.Loading -> {
                            dialogView.prgIndicator.visibility = View.VISIBLE
                            dialogView.deleteFlat.isEnabled = false
                            dialogView.saveFlat.isEnabled = false
                        }
                    }
                }
            }
        }
    }

    private fun saveFlat() {
        val newFlat = Flat(
            null,
            dialogView.flatName.text.toString(),
            dialogView.flatAddress.text.toString(),
            dialogView.flatFloor.text.toString().toInt()
        )

        viewModel.addFlat(newFlat).observe(viewLifecycleOwner) { resource ->
            if (resource != null) {
                when (resource) {
                    is Resource.Success -> {
                        dialog.hide()
                        dialogView.prgIndicator.visibility = View.GONE
                        dialogView.deleteFlat.isEnabled = true
                        dialogView.saveFlat.isEnabled = true
                    }
                    is Resource.Error -> {
                        Toast.makeText(
                            requireContext(),
                            resource.message,
                            Toast.LENGTH_LONG
                        ).show()
                        dialogView.prgIndicator.visibility = View.GONE
                        dialogView.deleteFlat.isEnabled = true
                        dialogView.saveFlat.isEnabled = true
                    }
                    is Resource.Loading -> {
                        dialogView.prgIndicator.visibility = View.VISIBLE
                        dialogView.deleteFlat.isEnabled = false
                        dialogView.saveFlat.isEnabled = false
                    }
                }
            }
        }
    }

    private fun updateFlat(flat: Flat) {
        // Check if we change something in the object
        if (flat.name == dialogView.flatName.text.toString() &&
            flat.address == dialogView.flatAddress.text.toString() &&
            flat.floor == dialogView.flatFloor.text.toString().toInt()
        ) {
            dialog.hide()
        } else {
            // Update Flat
            flat.name = dialogView.flatName.text.toString()
            flat.address = dialogView.flatAddress.text.toString()
            flat.floor = dialogView.flatFloor.text.toString().toInt()

            viewModel.updateFlat(flat).observe(viewLifecycleOwner) { resource ->
                if (resource != null) {
                    when (resource) {
                        is Resource.Success -> {
                            dialog.hide()
                            dialogView.prgIndicator.visibility = View.GONE
                            dialogView.deleteFlat.isEnabled = true
                            dialogView.saveFlat.isEnabled = true
                        }
                        is Resource.Error -> {
                            Toast.makeText(
                                requireContext(),
                                resource.message,
                                Toast.LENGTH_LONG
                            ).show()
                            dialogView.prgIndicator.visibility = View.GONE
                            dialogView.deleteFlat.isEnabled = true
                            dialogView.saveFlat.isEnabled = true
                        }
                        is Resource.Loading -> {
                            dialogView.prgIndicator.visibility = View.VISIBLE
                            dialogView.deleteFlat.isEnabled = false
                            dialogView.saveFlat.isEnabled = false
                        }
                    }
                }
            }
        }
    }

    private fun showForm(flat: Flat?) {
        dialogView.prgIndicator.visibility = View.GONE
        dialogView.saveFlat.isEnabled = true
        dialogView.deleteFlat.isEnabled = true

        dialogView.flatName.removeTextChangedListener(textWatcher)
        dialogView.flatAddress.removeTextChangedListener(textWatcher)
        dialogView.flatFloor.removeTextChangedListener(textWatcher)
        dialogView.flatName.error = null
        dialogView.flatAddress.error = null
        dialogView.flatFloor.error = null

        if (flat == null) {
            dialogView.flatName.addTextChangedListener(textWatcher)
            dialogView.flatAddress.addTextChangedListener(textWatcher)
            dialogView.flatFloor.addTextChangedListener(textWatcher)
            dialogView.flatName.setText("")
            dialogView.flatAddress.setText("")
            dialogView.flatFloor.setText("")
            dialogView.deleteFlat.visibility = View.GONE
            dialogView.saveFlat.setText(R.string.save)
            dialogView.flatName.requestFocus()
        } else {
            dialogView.flatName.setText(flat.name)
            dialogView.flatAddress.setText(flat.address)
            dialogView.flatFloor.setText(flat.floor.toString())
            dialogView.flatName.clearFocus()
            dialogView.flatAddress.clearFocus()
            dialogView.flatFloor.clearFocus()
            dialogView.deleteFlat.visibility = View.VISIBLE
            dialogView.saveFlat.setText(R.string.update)
            dialogView.flatName.addTextChangedListener(textWatcher)
            dialogView.flatAddress.addTextChangedListener(textWatcher)
            dialogView.flatFloor.addTextChangedListener(textWatcher)
        }

        dialog.show()
    }

    private fun validateForm() {
        val flatName = dialogView.flatName
        val flatAddress = dialogView.flatAddress
        val flatFloor = dialogView.flatFloor
        val saveFlat = dialogView.saveFlat
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
        return dialogView.flatName.error == null &&
                dialogView.flatAddress.error == null &&
                dialogView.flatFloor.error == null
    }
}