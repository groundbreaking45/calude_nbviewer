package com.nbviewer.presentation.render

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nbviewer.R
import com.nbviewer.presentation.viewer.CellUiModel
import com.nbviewer.presentation.viewer.OutputUiModel

/**
 * ViewHolder for Code cells.
 *
 * Renders:
 *   - Execution count label  (e.g., "[3]" or "[ ]")
 *   - Source code block with syntax highlighting via [SyntaxHighlighter]
 *   - Output section: text output, error output, image placeholder
 *
 * Output views are created dynamically and added to [outputContainer].
 * The container is cleared on each bind() to prevent stale views.
 *
 * PERFORMANCE: SyntaxHighlighter runs synchronously. For cells >500 lines,
 * consider moving to a coroutine and posting the result — the TextView
 * accepts a Spannable post-bind without layout issues.
 */
class CodeCellViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val executionCountView: TextView = view.findViewById(R.id.cell_code_execution_count)
    private val sourceView: TextView         = view.findViewById(R.id.cell_code_source)
    private val outputContainer: LinearLayout = view.findViewById(R.id.cell_code_output_container)

    fun bind(cell: CellUiModel.CodeCell) {
        // Execution count label
        executionCountView.text = cell.executionCount

        // Syntax-highlighted source
        sourceView.text = SyntaxHighlighter.highlight(cell.source, cell.language)

        // Outputs — rebuild from scratch on each bind
        outputContainer.removeAllViews()
        if (cell.outputs.isEmpty()) {
            outputContainer.visibility = View.GONE
        } else {
            outputContainer.visibility = View.VISIBLE
            cell.outputs.forEach { output ->
                val outputView = buildOutputView(output)
                outputContainer.addView(outputView)
            }
        }
    }

    private fun buildOutputView(output: OutputUiModel): View {
        val context = itemView.context
        return when (output) {
            is OutputUiModel.TextOutput -> TextView(context).apply {
                text = output.text
                typeface = Typeface.MONOSPACE
                textSize = 12f
                setTextColor(Color.parseColor("#333333"))
                setPadding(0, 4.dp, 0, 4.dp)
            }
            is OutputUiModel.ErrorOutput -> TextView(context).apply {
                text = buildString {
                    append(output.ename)
                    if (output.evalue.isNotBlank()) append(": ${output.evalue}")
                    if (output.traceback.isNotBlank()) {
                        append("\n\n")
                        append(output.traceback)
                    }
                }
                typeface = Typeface.MONOSPACE
                textSize = 11f
                setTextColor(Color.parseColor("#CC0000"))
                setBackgroundColor(Color.parseColor("#FFF5F5"))
                setPadding(8.dp, 8.dp, 8.dp, 8.dp)
            }
            is OutputUiModel.ImageOutput -> TextView(context).apply {
                text = context.getString(R.string.output_image_placeholder, output.mimeType)
                textSize = 12f
                setTextColor(Color.parseColor("#888888"))
                setPadding(0, 4.dp, 0, 4.dp)
            }
        }
    }

    private val Int.dp: Int
        get() = (this * itemView.context.resources.displayMetrics.density).toInt()

    companion object {
        fun create(parent: ViewGroup): CodeCellViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_cell_code, parent, false)
            return CodeCellViewHolder(view)
        }
    }
}
