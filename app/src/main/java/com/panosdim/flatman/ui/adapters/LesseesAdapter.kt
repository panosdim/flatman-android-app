package com.panosdim.flatman.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.panosdim.flatman.R
import com.panosdim.flatman.databinding.RowLesseeBinding
import com.panosdim.flatman.model.Flat
import com.panosdim.flatman.model.Lessee
import com.panosdim.flatman.utils.moneyFormat
import com.panosdim.flatman.utils.resolveColorAttr
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class LesseesAdapter(
    private val lesseesList: List<Lessee>,
    private val flatsList: List<Flat>,
    private val clickListener: (Lessee) -> Unit
) :
    RecyclerView.Adapter<LesseesAdapter.LesseeViewHolder>() {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    private val today = LocalDate.now()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LesseeViewHolder {
        val binding = RowLesseeBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return LesseeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LesseeViewHolder, position: Int) {
        with(holder) {
            with(lesseesList[position]) {
                binding.rowLesseeName.text = name
                binding.rowLesseeAddress.text = address
                binding.rowLesseePostalCode.text =
                    holder.itemView.context.getString(R.string.postal_code_title, postalCode)
                binding.rowLesseeRent.text = moneyFormat(rent)
                binding.rowLesseeTin.text =
                    holder.itemView.context.getString(R.string.tin_title, tin)
                binding.rowFlatName.text = flatsList.first { it.id == flatId }.name
                binding.rowLesseeFrom.text = dateFormatter.format(LocalDate.parse(from))
                binding.rowLesseeUntil.text = dateFormatter.format(LocalDate.parse(until))
                if (today.isAfter(LocalDate.parse(until))) {
                    binding.crdLessee.setCardBackgroundColor(
                        holder.itemView.context.resolveColorAttr(
                            R.attr.colorExpense
                        )
                    )
                }
                holder.itemView.setOnClickListener { clickListener(this) }
            }
        }
    }

    override fun getItemCount() = lesseesList.size

    inner class LesseeViewHolder(val binding: RowLesseeBinding) :
        RecyclerView.ViewHolder(binding.root)
}