package com.panosdim.flatman.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.panosdim.flatman.R
import com.panosdim.flatman.flatsList
import com.panosdim.flatman.model.Flat
import kotlinx.android.synthetic.main.row_flat.view.*


class FlatsAdapter(
    private val clickListener: (Flat) -> Unit
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // LayoutInflater: takes ID from layout defined in XML.
        // Instantiates the layout XML into corresponding View objects.
        // Use context from main app -> also supplies theme layout values!
        val inflater = LayoutInflater.from(parent.context)
        // Inflate XML. Last parameter: don't immediately attach new view to the parent view group
        val view = inflater.inflate(R.layout.row_flat, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // Populate ViewHolder with data that corresponds to the position in the list
        // which we are told to load
        (holder as ExpenseViewHolder).bind(flatsList.value!![position], clickListener)
    }

    override fun getItemCount() = flatsList.value!!.size

    class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(flat: Flat, clickListener: (Flat) -> Unit) {
            itemView.rowFlatName.text = flat.name
            itemView.rowFlatAddress.text = flat.address
            itemView.rowFlatFloor.text = itemView.context.getString(R.string.floor, flat.floor)
            itemView.setOnClickListener { clickListener(flat) }
        }
    }
}