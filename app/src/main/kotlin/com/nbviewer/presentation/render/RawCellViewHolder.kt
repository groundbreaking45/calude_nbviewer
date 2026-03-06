package com.nbviewer.presentation.render

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nbviewer.R
import com.nbviewer.presentation.viewer.CellUiModel

/**
 * ViewHolder for Raw cells. Displays source as plain monospace text.
 * Raw cells are used for rst, latex, or custom notebook formats — not rendered specially in v1.
 */
class RawCellViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val textView: TextView = view.findViewById(R.id.cell_raw_text)

    fun bind(cell: CellUiModel.RawCell) {
        textView.text = cell.source.ifBlank { "(empty raw cell)" }
    }

    companion object {
        fun create(parent: ViewGroup): RawCellViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_cell_raw, parent, false)
            return RawCellViewHolder(view)
        }
    }
}
