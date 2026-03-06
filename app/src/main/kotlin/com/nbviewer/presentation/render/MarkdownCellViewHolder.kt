package com.nbviewer.presentation.render

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nbviewer.R
import com.nbviewer.presentation.viewer.CellUiModel
import io.noties.markwon.Markwon

/**
 * ViewHolder for Markdown cells.
 *
 * Uses Markwon to render raw Markdown into a native Android Spannable
 * applied directly to a TextView. No WebView involved.
 *
 * Markwon is instantiated once per ViewHolder creation and reused across
 * bind() calls — it is stateless and safe to reuse.
 *
 * FUTURE: Add Markwon extensions (tables, strikethrough, task lists, math)
 * by registering plugins in the Markwon.builder() call. Zero ViewHolder changes.
 */
class MarkdownCellViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val textView: TextView = view.findViewById(R.id.cell_markdown_text)
    private val markwon: Markwon = Markwon.create(view.context)

    fun bind(cell: CellUiModel.MarkdownCell) {
        markwon.setMarkdown(textView, cell.rawMarkdown)
    }

    companion object {
        fun create(parent: ViewGroup): MarkdownCellViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_cell_markdown, parent, false)
            return MarkdownCellViewHolder(view)
        }
    }
}
