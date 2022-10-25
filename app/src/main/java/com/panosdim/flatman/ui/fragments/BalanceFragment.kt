package com.panosdim.flatman.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.panosdim.flatman.R
import com.panosdim.flatman.api.data.Resource
import com.panosdim.flatman.databinding.FragmentBalanceBinding
import com.panosdim.flatman.model.Balance
import com.panosdim.flatman.model.Flat
import com.panosdim.flatman.ui.adapters.BalanceAdapter
import com.panosdim.flatman.ui.dialogs.BalanceDialog
import com.panosdim.flatman.viewmodel.BalanceViewModel
import com.panosdim.flatman.viewmodel.FlatViewModel


class BalanceFragment : Fragment() {
    private var _binding: FragmentBalanceBinding? = null
    private val binding get() = _binding!!
    private val flatViewModel: FlatViewModel by viewModels()
    private lateinit var flatSelectAdapter: ArrayAdapter<Flat>
    private val viewModel: BalanceViewModel by viewModels()
    private val balanceDialog: BalanceDialog = BalanceDialog()

    private fun balanceItemClicked(balance: Balance) {
        balanceDialog.showNow(childFragmentManager, BalanceDialog.TAG)
        balanceDialog.showForm(balance)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getAllBalance().observe(viewLifecycleOwner) { resource ->
            if (resource != null) {
                when (resource) {
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.rvBalance.visibility = View.VISIBLE
                        updateBalanceAdapter()
                        viewModel.getAllBalance().removeObservers(viewLifecycleOwner)
                    }
                    is Resource.Error -> {
                        Toast.makeText(
                            requireContext(),
                            resource.message,
                            Toast.LENGTH_LONG
                        ).show()
                        binding.progressBar.visibility = View.GONE
                        binding.rvBalance.visibility = View.VISIBLE
                    }
                    is Resource.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.rvBalance.visibility = View.GONE
                    }
                }
            }
        }

        viewModel.balance.observe(viewLifecycleOwner) {
            updateBalanceAdapter()
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshBalance()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBalanceBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val rvBalance = binding.rvBalance
        rvBalance.setHasFixedSize(true)
        rvBalance.layoutManager = LinearLayoutManager(binding.root.context)

        binding.addNewBalance.setOnClickListener {
            balanceDialog.showNow(childFragmentManager, BalanceDialog.TAG)
            balanceDialog.showForm(null)
        }

        flatViewModel.getFlats().observe(viewLifecycleOwner) { resource ->
            if (resource != null) {
                when (resource) {
                    is Resource.Success -> {
                        if (resource.data != null && resource.data.isNotEmpty()) {
                            binding.addNewBalance.isEnabled = true
                            flatSelectAdapter =
                                ArrayAdapter(requireContext(), R.layout.list_item, resource.data)

                            updateBalanceAdapter()
                        } else {
                            binding.addNewBalance.isEnabled = false
                            binding.rvBalance.adapter =
                                BalanceAdapter(mutableListOf()) { balanceItem: Balance ->
                                    balanceItemClicked(
                                        balanceItem
                                    )
                                }
                        }
                    }
                    is Resource.Error -> {}
                    is Resource.Loading -> {}
                }
            }
        }

        return root
    }

    private fun updateBalanceAdapter() {
        viewModel.balance.value?.let {
            binding.rvBalance.adapter =
                BalanceAdapter(it) { balanceItem: Balance -> balanceItemClicked(balanceItem) }
        } ?: kotlin.run {
            binding.rvBalance.adapter =
                BalanceAdapter(mutableListOf()) { balanceItem: Balance ->
                    balanceItemClicked(
                        balanceItem
                    )
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}