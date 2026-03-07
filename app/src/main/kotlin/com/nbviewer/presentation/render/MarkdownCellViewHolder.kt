package com.nbviewer.presentation.render

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nbviewer.R
import com.nbviewer.presentation.viewer.CellUiModel
import org.commonmark.node.Document
import org.commonmark.parser.Parser

class MarkdownCellViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val textView: TextView = view.findViewById(R.id.cell_markdown_text)
    private val parser = Parser.builder().build()

    fun bind(cell: CellUiModel.MarkdownCell) {
        val document = parser.parse(cell.rawMarkdown) as Document
        val renderer = MarkdownSpannedRenderer(itemView.context)
        textView.text = renderer.render(document)
    }

    companion object {
        fun create(parent: ViewGroup): MarkdownCellViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_cell_markdown, parent, false)
            return MarkdownCellViewHolder(view)
        }
    }
}