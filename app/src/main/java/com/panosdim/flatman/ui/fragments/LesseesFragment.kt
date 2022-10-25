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
import com.panosdim.flatman.viewmodel.FlatViewModel
import com.panosdim.flatman.viewmodel.LesseeViewModel


class LesseesFragment : Fragment() {
    private var _binding: FragmentLesseesBinding? = null
    private val binding get() = _binding!!
    private val flatViewModel: FlatViewModel by viewModels()
    private val lesseeDialog: LesseeDialog = LesseeDialog()
    private val viewModel: LesseeViewModel by viewModels()

    private fun lesseeItemClicked(lessee: Lessee) {
        lesseeDialog.showNow(childFragmentManager, LesseeDialog.TAG)
        lesseeDialog.showForm(lessee)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getAllLessee().observe(viewLifecycleOwner) { resource ->
            if (resource != null) {
                when (resource) {
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.rvLessees.visibility = View.VISIBLE
                        updateLesseeAdapter()
                        viewModel.getAllLessee().removeObservers(viewLifecycleOwner)
                    }
                    is Resource.Error -> {
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

        viewModel.lessee.observe(viewLifecycleOwner) {
            updateLesseeAdapter()
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshLessee()
            binding.swipeRefresh.isRefreshing = false
        }
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

        flatViewModel.flats.observe(viewLifecycleOwner) {
            if (it != null && it.isNotEmpty()) {
                binding.addNewLessee.isEnabled = true
                updateLesseeAdapter()
            } else {
                binding.addNewLessee.isEnabled = false
                binding.rvLessees.adapter =
                    LesseesAdapter(mutableListOf(), mutableListOf()) { lesseeItem: Lessee ->
                        lesseeItemClicked(
                            lesseeItem
                        )
                    }
            }
        }

        return root
    }

    private fun updateLesseeAdapter() {
        viewModel.lessee.value?.let { lessees ->
            flatViewModel.flats.value?.let { flats ->
                binding.rvLessees.adapter =
                    LesseesAdapter(
                        lessees,
                        flats
                    ) { lesseeItem: Lessee -> lesseeItemClicked(lesseeItem) }
            } ?: kotlin.run {
                binding.rvLessees.adapter =
                    LesseesAdapter(
                        mutableListOf(),
                        mutableListOf()
                    ) { lesseeItem: Lessee -> lesseeItemClicked(lesseeItem) }
            }
        } ?: kotlin.run {
            binding.rvLessees.adapter =
                LesseesAdapter(
                    mutableListOf(),
                    mutableListOf()
                ) { lesseeItem: Lessee -> lesseeItemClicked(lesseeItem) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}