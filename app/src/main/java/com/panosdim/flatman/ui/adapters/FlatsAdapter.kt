package com.panosdim.flatman.ui.adapters

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.panosdim.flatman.databinding.RowFlatBinding
import com.panosdim.flatman.model.Flat
import com.panosdim.flatman.utils.setBottomMargin


class FlatsAdapter(private val clickListener: (Flat) -> Unit) :
    ListAdapter<Flat, FlatsAdapter.FlatViewHolder>(object : DiffUtil.ItemCallback<Flat>() {
        override fun areItemsTheSame(oldItem: Flat, newItem: Flat): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Flat, newItem: Flat): Boolean {
            return oldItem == newItem
        }
    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlatViewHolder {
        val binding = RowFlatBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return FlatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FlatViewHolder, position: Int) {
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
                binding.rowFlatName.text = name
                binding.rowFlatAddress.text = address
                binding.rowFlatFloor.text =
                    itemView.context.getString(com.panosdim.flatman.R.string.floor, floor)
                holder.itemView.setOnClickListener { clickListener(this) }
            }
        }
    }

    inner class FlatViewHolder(val binding: RowFlatBinding) :
        RecyclerView.ViewHolder(binding.root)
}