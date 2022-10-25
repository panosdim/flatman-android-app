package com.panosdim.flatman.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.panosdim.flatman.R
import com.panosdim.flatman.databinding.RowBalanceBinding
import com.panosdim.flatman.model.Balance
import com.panosdim.flatman.utils.moneyFormat
import com.panosdim.flatman.utils.resolveColorAttr
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class BalanceAdapter(
    private val balanceList: List<Balance>,
    private val clickListener: (Balance) -> Unit
) :
    RecyclerView.Adapter<BalanceAdapter.BalanceViewHolder>() {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BalanceViewHolder {
        val binding = RowBalanceBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return BalanceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BalanceViewHolder, position: Int) {
        with(holder) {
            with(balanceList[position]) {
                binding.rowBalanceDate.text = dateFormatter.format(LocalDate.parse(date))
                binding.rowBalanceAmount.text = moneyFormat(amount)
                binding.rowBalanceComment.text = comment
                if (amount < 0.0f) {
                    binding.rowBalanceAmount.setTextColor(itemView.context.resolveColorAttr(R.attr.colorExpense))
                } else {
                    binding.rowBalanceAmount.setTextColor(itemView.context.resolveColorAttr(R.attr.colorIncome))
                }
                itemView.setOnClickListener { clickListener(this) }
                holder.itemView.setOnClickListener { clickListener(this) }
            }
        }
    }

    override fun getItemCount() = balanceList.size

    inner class BalanceViewHolder(val binding: RowBalanceBinding) :
        RecyclerView.ViewHolder(binding.root)
}