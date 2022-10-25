package com.panosdim.flatman.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.panosdim.flatman.databinding.RowFlatBinding
import com.panosdim.flatman.model.Flat


class FlatsAdapter(
    private val flatsList: List<Flat>,
    private val clickListener: (Flat) -> Unit
) :
    RecyclerView.Adapter<FlatsAdapter.FlatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlatViewHolder {
        val binding = RowFlatBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return FlatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FlatViewHolder, position: Int) {
        with(holder) {
            with(flatsList[position]) {
                binding.rowFlatName.text = name
                binding.rowFlatAddress.text = address
                binding.rowFlatFloor.text =
                    itemView.context.getString(com.panosdim.flatman.R.string.floor, floor)
                holder.itemView.setOnClickListener { clickListener(this) }
            }
        }
    }

    override fun getItemCount() = flatsList.size

    inner class FlatViewHolder(val binding: RowFlatBinding) :
        RecyclerView.ViewHolder(binding.root)
}