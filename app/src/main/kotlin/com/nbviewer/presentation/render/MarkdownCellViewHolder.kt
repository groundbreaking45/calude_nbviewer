package com.nbviewer.presentation.render

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nbviewer.R
import com.nbviewer.presentation.viewer.CellUiModel
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

class MarkdownCellViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val textView: TextView = view.findViewById(R.id.cell_markdown_text)

    private val parser = Parser.builder().build()
    private val renderer = HtmlRenderer.builder().build()

    fun bind(cell: CellUiModel.MarkdownCell) {
        val document = parser.parse(cell.rawMarkdown)
        val html = renderer.render(document)
        @Suppress("DEPRECATION")
        textView.text = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
        } else {
            Html.fromHtml(html)
        }
    }

    companion object {
        fun create(parent: ViewGroup): MarkdownCellViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_cell_markdown, parent, false)
            return MarkdownCellViewHolder(view)
        }
    }
}
