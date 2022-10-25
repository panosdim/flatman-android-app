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
import com.panosdim.flatman.databinding.FragmentFlatsBinding
import com.panosdim.flatman.model.Flat
import com.panosdim.flatman.ui.adapters.FlatsAdapter
import com.panosdim.flatman.ui.dialogs.FlatDialog
import com.panosdim.flatman.viewmodel.FlatViewModel


class FlatsFragment : Fragment() {
    private var _binding: FragmentFlatsBinding? = null
    private val binding get() = _binding!!
    private val flatsAdapter = FlatsAdapter { flatItem: Flat -> flatItemClicked(flatItem) }
    private val viewModel: FlatViewModel by viewModels()
    private val flatDialog: FlatDialog = FlatDialog()

    private fun flatItemClicked(flat: Flat) {
        flatDialog.showNow(childFragmentManager, FlatDialog.TAG)
        flatDialog.showForm(flat)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFlatsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        viewModel.getFlats().observe(viewLifecycleOwner) { resource ->
            if (resource != null) {
                when (resource) {
                    is Resource.Success -> {
                        resource.data?.let { flats ->
                            flatsAdapter.submitList(flats)
                        }

                        binding.progressBar.visibility = View.GONE
                        binding.rvFlats.visibility = View.VISIBLE
                    }
                    is Resource.Error -> {
                        Toast.makeText(
                            requireContext(),
                            resource.message,
                            Toast.LENGTH_LONG
                        ).show()
                        binding.progressBar.visibility = View.GONE
                        binding.rvFlats.visibility = View.VISIBLE
                    }
                    is Resource.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.rvFlats.visibility = View.GONE
                    }
                }
            }
        }

        val rvFlats = binding.rvFlats
        rvFlats.setHasFixedSize(true)
        rvFlats.layoutManager = LinearLayoutManager(root.context)
        rvFlats.adapter = flatsAdapter

        binding.addNewFlat.setOnClickListener {
            flatDialog.showNow(childFragmentManager, FlatDialog.TAG)
            flatDialog.showForm(null)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshFlats()
            binding.swipeRefresh.isRefreshing = false
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}