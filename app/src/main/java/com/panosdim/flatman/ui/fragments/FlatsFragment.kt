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
    private val flatsViewAdapter =
        FlatsAdapter(mutableListOf()) { flatItem: Flat -> flatItemClicked(flatItem) }
    private val viewModel: FlatViewModel by viewModels()
    private val flatDialog: FlatDialog = FlatDialog()


    private fun flatItemClicked(flat: Flat) {
        flatDialog.showNow(childFragmentManager, FlatDialog.TAG)
        flatDialog.showForm(flat)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getAllFlats().observe(viewLifecycleOwner) { resource ->
            if (resource != null) {
                when (resource) {
                    is Resource.Success -> {
                        binding.rvFlats.adapter =
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

                        binding.progressBar.visibility = View.GONE
                        binding.rvFlats.visibility = View.VISIBLE

                        viewModel.getAllFlats().removeObservers(viewLifecycleOwner)
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

        viewModel.flats.observe(viewLifecycleOwner) {
            binding.rvFlats.adapter =
                FlatsAdapter(it) { flatItem: Flat -> flatItemClicked(flatItem) }
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshFlats()
            binding.swipeRefresh.isRefreshing = false
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFlatsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val rvFlats = binding.rvFlats
        rvFlats.setHasFixedSize(true)
        rvFlats.layoutManager = LinearLayoutManager(root.context)
        rvFlats.adapter = flatsViewAdapter

        binding.addNewFlat.setOnClickListener {
            flatDialog.showNow(childFragmentManager, FlatDialog.TAG)
            flatDialog.showForm(null)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}