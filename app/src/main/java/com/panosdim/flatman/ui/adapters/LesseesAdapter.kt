package com.panosdim.flatman.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.panosdim.flatman.R
import com.panosdim.flatman.model.Lessee
import com.panosdim.flatman.utils.moneyFormat
import kotlinx.android.synthetic.main.row_lessee.view.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class LesseesAdapter(
    private val lesseesList: List<Lessee>,
    private val clickListener: (Lessee) -> Unit
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // LayoutInflater: takes ID from layout defined in XML.
        // Instantiates the layout XML into corresponding View objects.
        // Use context from main app -> also supplies theme layout values!
        val inflater = LayoutInflater.from(parent.context)
        // Inflate XML. Last parameter: don't immediately attach new view to the parent view group
        val view = inflater.inflate(R.layout.row_lessee, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // Populate ViewHolder with data that corresponds to the position in the list
        // which we are told to load
        (holder as ExpenseViewHolder).bind(lesseesList[position], clickListener)
    }

    override fun getItemCount() = lesseesList.size

    class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        private val today = LocalDate.now()

        fun bind(lessee: Lessee, clickListener: (Lessee) -> Unit) {
            itemView.rowLesseeName.text = lessee.name
            itemView.rowLesseeAddress.text = lessee.address
            itemView.rowLesseePostalCode.text =
                itemView.context.getString(R.string.postal_code_title, lessee.postalCode)
            itemView.rowLesseeRent.text = moneyFormat(lessee.rent)
            itemView.rowLesseeTin.text = itemView.context.getString(R.string.tin_title, lessee.tin)
            itemView.rowLesseeFrom.text = dateFormatter.format(LocalDate.parse(lessee.from))
            itemView.rowLesseeUntil.text = dateFormatter.format(LocalDate.parse(lessee.until))
            if (today.isAfter(LocalDate.parse(lessee.until))) {
                itemView.crdLessee.setCardBackgroundColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.red
                    )
                )
            }
            itemView.setOnClickListener { clickListener(lessee) }
        }
    }
}