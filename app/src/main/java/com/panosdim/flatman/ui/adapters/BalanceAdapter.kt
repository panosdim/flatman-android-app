package com.panosdim.flatman.ui.adapters

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.panosdim.flatman.R
import com.panosdim.flatman.databinding.RowBalanceBinding
import com.panosdim.flatman.model.Balance
import com.panosdim.flatman.model.Flat
import com.panosdim.flatman.utils.moneyFormat
import com.panosdim.flatman.utils.resolveColorAttr
import com.panosdim.flatman.utils.setBottomMargin
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class BalanceAdapter(
    private val flatsList: List<Flat>,
    private val clickListener: (Balance) -> Unit
) :
    ListAdapter<Balance, BalanceAdapter.BalanceViewHolder>(object :
        DiffUtil.ItemCallback<Balance>() {
        override fun areItemsTheSame(oldItem: Balance, newItem: Balance): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Balance, newItem: Balance): Boolean {
            return oldItem == newItem
        }

    }) {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BalanceViewHolder {
        val binding = RowBalanceBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return BalanceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BalanceViewHolder, position: Int) {
        if (position + 1 == itemCount) {
            setBottomMargin(
                holder.itemView,
                (64 * Resources.getSystem().displayMetrics.density).toInt()
            )
        } else {
            setBottomMargin(holder.itemView, 0)
        }

        with(holder) {
            with(getItem(position)) {
                binding.rowBalanceDate.text = dateFormatter.format(LocalDate.parse(date))
                binding.rowBalanceAmount.text = moneyFormat(amount)
                binding.rowBalanceComment.text = comment
                binding.rowFlatName.text = flatsList.first { it.id == flatId }.name
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

    inner class BalanceViewHolder(val binding: RowBalanceBinding) :
        RecyclerView.ViewHolder(binding.root)
}