package com.panosdim.flatman.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.panosdim.flatman.api.data.Resource
import com.panosdim.flatman.databinding.FragmentLesseesBinding
import com.panosdim.flatman.model.Lessee
import com.panosdim.flatman.ui.adapters.LesseesAdapter
import com.panosdim.flatman.ui.dialogs.LesseeDialog
import com.panosdim.flatman.ui.dialogs.LesseesFilterDialog
import com.panosdim.flatman.viewmodel.FlatViewModel
import com.panosdim.flatman.viewmodel.LesseeViewModel


class LesseesFragment : Fragment() {
    private var _binding: FragmentLesseesBinding? = null
    private val binding get() = _binding!!
    private val flatViewModel: FlatViewModel by viewModels()
    private val lesseeDialog: LesseeDialog = LesseeDialog()
    private val viewModel: LesseeViewModel by viewModels()
    private var lesseesAdapter = LesseesAdapter(mutableListOf()) { lesseeItem: Lessee ->
        lesseeItemClicked(
            lesseeItem
        )
    }
    private val lesseesFilterDialog: LesseesFilterDialog = LesseesFilterDialog()

    private fun lesseeItemClicked(lessee: Lessee) {
        lesseeDialog.showNow(childFragmentManager, LesseeDialog.TAG)
        lesseeDialog.showForm(lessee)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLesseesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val rvLessees = binding.rvLessees
        rvLessees.setHasFixedSize(true)
        rvLessees.layoutManager = LinearLayoutManager(binding.root.context)

        binding.addNewLessee.setOnClickListener {
            lesseeDialog.showNow(childFragmentManager, LesseeDialog.TAG)
            lesseeDialog.showForm(null)
        }

        binding.filterLessees.setOnClickListener {
            lesseesFilterDialog.showNow(childFragmentManager, LesseesFilterDialog.TAG)
        }

        flatViewModel.getFlats().observe(viewLifecycleOwner) { resource ->
            if (resource != null) {
                when (resource) {
                    is Resource.Success -> {
                        if (resource.data != null && resource.data.isNotEmpty()) {
                            binding.addNewLessee.isEnabled = true
                            lesseesAdapter = LesseesAdapter(
                                resource.data
                            ) { lesseeItem: Lessee ->
                                lesseeItemClicked(
                                    lesseeItem
                                )
                            }
                            binding.rvLessees.adapter = lesseesAdapter
                            lesseeDialog.flats = resource.data
                            lesseesFilterDialog.flats = resource.data

                            // Fetch Lessees
                            viewModel.getLessees().observe(viewLifecycleOwner) { res ->
                                if (res != null) {
                                    when (res) {
                                        is Resource.Success -> {
                                            binding.progressBar.visibility = View.GONE
                                            binding.rvLessees.visibility = View.VISIBLE

                                            res.data?.let { lessees ->
                                                lesseesAdapter.submitList(lessees)
                                                lesseesFilterDialog.lesseesAdapter = lesseesAdapter
                                                rvLessees.post { rvLessees.scrollToPosition(0) }
                                            }
                                        }
                                        is Resource.Error -> {
                                            Toast.makeText(
                                                requireContext(),
                                                res.message,
                                                Toast.LENGTH_LONG
                                            ).show()
                                            binding.progressBar.visibility = View.GONE
                                            binding.rvLessees.visibility = View.VISIBLE
                                        }
                                        is Resource.Loading -> {
                                            binding.progressBar.visibility = View.VISIBLE
                                            binding.rvLessees.visibility = View.GONE
                                        }
                                    }
                                }
                            }
                        } else {
                            binding.addNewLessee.isEnabled = false
                        }
                    }
                    is Resource.Error -> {
                        binding.addNewLessee.isEnabled = false
                        Toast.makeText(
                            requireContext(),
                            resource.message,
                            Toast.LENGTH_LONG
                        ).show()
                        binding.progressBar.visibility = View.GONE
                        binding.rvLessees.visibility = View.VISIBLE
                    }
                    is Resource.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.rvLessees.visibility = View.GONE
                    }
                }
            }
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshLessee()
            binding.swipeRefresh.isRefreshing = false
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}