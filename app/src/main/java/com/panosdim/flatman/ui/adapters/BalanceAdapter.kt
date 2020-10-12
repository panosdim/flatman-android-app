package com.panosdim.flatman.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.panosdim.flatman.R
import com.panosdim.flatman.model.Balance
import com.panosdim.flatman.utils.moneyFormat
import kotlinx.android.synthetic.main.row_balance.view.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class BalanceAdapter(
    private val balanceList: List<Balance>,
    private val clickListener: (Balance) -> Unit
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // LayoutInflater: takes ID from layout defined in XML.
        // Instantiates the layout XML into corresponding View objects.
        // Use context from main app -> also supplies theme layout values!
        val inflater = LayoutInflater.from(parent.context)
        // Inflate XML. Last parameter: don't immediately attach new view to the parent view group
        val view = inflater.inflate(R.layout.row_balance, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // Populate ViewHolder with data that corresponds to the position in the list
        // which we are told to load
        (holder as ExpenseViewHolder).bind(balanceList[position], clickListener)
    }

    override fun getItemCount() = balanceList.size

    class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

        fun bind(balance: Balance, clickListener: (Balance) -> Unit) {
            itemView.rowBalanceDate.text = dateFormatter.format(LocalDate.parse(balance.date))
            itemView.rowBalanceAmount.text = moneyFormat(balance.amount)
            itemView.rowBalanceComment.text = balance.comment
            if (balance.amount < 0.0f) {
                itemView.rowBalanceAmount.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.red
                    )
                )
            } else {
                itemView.rowBalanceAmount.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.green
                    )
                )
            }
            itemView.setOnClickListener { clickListener(balance) }
        }
    }
}