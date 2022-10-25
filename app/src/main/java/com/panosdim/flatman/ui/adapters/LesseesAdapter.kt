package com.panosdim.flatman.ui.adapters

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.panosdim.flatman.R
import com.panosdim.flatman.databinding.RowLesseeBinding
import com.panosdim.flatman.model.Flat
import com.panosdim.flatman.model.Lessee
import com.panosdim.flatman.utils.moneyFormat
import com.panosdim.flatman.utils.resolveColorAttr
import com.panosdim.flatman.utils.setBottomMargin
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class LesseesAdapter(
    private val flatsList: List<Flat>,
    private val clickListener: (Lessee) -> Unit
) :
    ListAdapter<Lessee, LesseesAdapter.LesseeViewHolder>(LesseeDiffCallback()) {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    private val today = LocalDate.now()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LesseeViewHolder {
        val binding = RowLesseeBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return LesseeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LesseeViewHolder, position: Int) {
        if (position + 1 == itemCount) {
            // It is the last item of the list
            // Set bottom margin
            setBottomMargin(
                holder.itemView,
                (64 * Resources.getSystem().displayMetrics.density).toInt()
            )
        } else {
            // Reset bottom margin
            setBottomMargin(holder.itemView, 0)
        }

        with(holder) {
            with(getItem(position)) {
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

    inner class LesseeViewHolder(val binding: RowLesseeBinding) :
        RecyclerView.ViewHolder(binding.root)
}

class LesseeDiffCallback : DiffUtil.ItemCallback<Lessee>() {
    override fun areItemsTheSame(oldItem: Lessee, newItem: Lessee): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Lessee, newItem: Lessee): Boolean {
        return oldItem == newItem
    }
}