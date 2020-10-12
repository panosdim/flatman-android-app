package com.panosdim.flatman.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.panosdim.flatman.R
import com.panosdim.flatman.flatsList
import com.panosdim.flatman.model.Flat
import com.panosdim.flatman.repository
import com.panosdim.flatman.ui.adapters.FlatsAdapter
import com.panosdim.flatman.ui.login.afterTextChanged
import kotlinx.android.synthetic.main.dialog_flat.view.*
import kotlinx.android.synthetic.main.fragment_flats.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException


class FlatsFragment : Fragment() {
    private val flatsViewAdapter =
        FlatsAdapter { flatItem: Flat -> flatItemClicked(flatItem) }
    private lateinit var dialog: BottomSheetDialog
    private lateinit var dialogView: View
    private var flat: Flat? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private fun flatItemClicked(flatItem: Flat) {
        flat = flatItem
        showForm(flat)
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

        dialogView.flatName.afterTextChanged {
            validateForm()
        }

        dialogView.flatAddress.afterTextChanged {
            validateForm()
        }

        dialogView.flatFloor.afterTextChanged {
            validateForm()
        }

        dialogView.saveFlat.setOnClickListener {
            dialogView.prgIndicator.visibility = View.VISIBLE
            dialogView.saveFlat.isEnabled = false
            dialogView.deleteFlat.isEnabled = false

            flat?.let {
                updateFlat(it)
            } ?: kotlin.run {
                saveFlat()
            }
        }

        dialogView.deleteFlat.setOnClickListener {
            flat?.let {
                scope.launch {
                    dialogView.prgIndicator.visibility = View.VISIBLE
                    dialogView.deleteFlat.isEnabled = false
                    dialogView.saveFlat.isEnabled = false
                    try {
                        withContext(Dispatchers.IO) {
                            val response = repository.deleteFlat(it.id!!)
                            when (response.code()) {
                                204 -> {
                                    val temp = flatsList.value
                                    temp!!.remove(it)
                                    flatsList.postValue(temp)
                                }
                                404 -> {
                                    Toast.makeText(
                                        requireContext(),
                                        "Error deleting flat. Flat not found.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    dialogView.prgIndicator.visibility = View.GONE
                                    dialogView.deleteFlat.isEnabled = true
                                    dialogView.saveFlat.isEnabled = true
                                }
                            }
                        }
                        flatsViewAdapter.notifyDataSetChanged()
                        dialog.hide()
                    } catch (ex: HttpException) {
                        Toast.makeText(
                            requireContext(),
                            "Error deleting flat.",
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
                        dialogView.saveFlat.isEnabled = true
                        dialogView.deleteFlat.isEnabled = true
                    }
                }
            }
        }

        return root
    }

    private fun saveFlat() {
        val newFlat = Flat(
            null,
            dialogView.flatName.text.toString(),
            dialogView.flatAddress.text.toString(),
            dialogView.flatFloor.text.toString().toInt()
        )

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val response = repository.createNewFlat(newFlat)
                    val temp = flatsList.value
                    temp?.add(response)
                    flatsList.postValue(temp)
                }
                flatsViewAdapter.notifyDataSetChanged()
                dialog.hide()
            } catch (e: HttpException) {
                Toast.makeText(requireContext(), "Error saving flat.", Toast.LENGTH_SHORT)
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
                dialogView.saveFlat.isEnabled = true
                dialogView.deleteFlat.isEnabled = true
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

            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val response = repository.updateFlat(flat.id!!, flat)
                        val temp = flatsList.value
                        val index = temp!!.indexOfFirst { (id) -> id == response.id }
                        temp[index] = response
                        flatsList.postValue(temp)
                    }
                    flatsViewAdapter.notifyDataSetChanged()
                    dialog.hide()
                } catch (e: HttpException) {
                    Toast.makeText(
                        requireContext(),
                        "Error saving flat.",
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
                    dialogView.saveFlat.isEnabled = true
                    dialogView.deleteFlat.isEnabled = true
                }
            }
        }
    }

    private fun showForm(flat: Flat?) {
        dialogView.prgIndicator.visibility = View.GONE
        dialogView.saveFlat.isEnabled = true
        dialogView.deleteFlat.isEnabled = true
        if (flat == null) {
            dialogView.flatName.setText("")
            dialogView.flatAddress.setText("")
            dialogView.flatFloor.setText("")
            dialogView.deleteFlat.visibility = View.GONE
            dialogView.saveFlat.setText(R.string.save)
        } else {
            dialogView.flatName.setText(flat.name)
            dialogView.flatAddress.setText(flat.address)
            dialogView.flatFloor.setText(flat.floor.toString())
            dialogView.deleteFlat.visibility = View.VISIBLE
            dialogView.saveFlat.setText(R.string.update)
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

        if (flatName.text!!.isEmpty()) {
            flatName.error = getString(R.string.error_field_required)
            saveFlat.isEnabled = false
        }

        if (flatAddress.text!!.isEmpty()) {
            flatAddress.error = getString(R.string.error_field_required)
            saveFlat.isEnabled = false
        }

        if (flatFloor.text!!.isEmpty()) {
            flatFloor.error = getString(R.string.error_field_required)
            saveFlat.isEnabled = false
        }
    }
}