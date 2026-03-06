package com.nbviewer.presentation.viewer

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.nbviewer.presentation.render.CodeCellViewHolder
import com.nbviewer.presentation.render.MarkdownCellViewHolder
import com.nbviewer.presentation.render.RawCellViewHolder

/**
 * RecyclerView adapter for the notebook cell list.
 *
 * Supports three view types: Markdown, Code, Raw.
 * Uses [DiffUtil] for efficient, animated list updates (important for large notebooks).
 *
 * Cell.Unknown is excluded from the UI model list in the ViewModel — this adapter
 * never receives unknown types and does not need to handle them.
 *
 * PERFORMANCE:
 * - setHasStableIds(true) + string-based IDs enables RecyclerView to skip rebind
 *   for cells that haven't changed during a re-load.
 * - ViewHolder recycling pools are shared automatically by RecyclerView.
 */
class NotebookAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var cells: List<CellUiModel> = emptyList()

    init {
        setHasStableIds(true)
    }

    // -------------------------------------------------------------------------
    // View type constants
    // -------------------------------------------------------------------------

    companion object {
        const val TYPE_MARKDOWN = 0
        const val TYPE_CODE     = 1
        const val TYPE_RAW      = 2
    }

    // -------------------------------------------------------------------------
    // Adapter overrides
    // -------------------------------------------------------------------------

    override fun getItemCount(): Int = cells.size

    override fun getItemId(position: Int): Long = cells[position].id.hashCode().toLong()

    override fun getItemViewType(position: Int): Int = when (cells[position]) {
        is CellUiModel.MarkdownCell -> TYPE_MARKDOWN
        is CellUiModel.CodeCell     -> TYPE_CODE
        is CellUiModel.RawCell      -> TYPE_RAW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            TYPE_MARKDOWN -> MarkdownCellViewHolder.create(parent)
            TYPE_CODE     -> CodeCellViewHolder.create(parent)
            else          -> RawCellViewHolder.create(parent)
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val cell = cells[position]) {
            is CellUiModel.MarkdownCell -> (holder as MarkdownCellViewHolder).bind(cell)
            is CellUiModel.CodeCell     -> (holder as CodeCellViewHolder).bind(cell)
            is CellUiModel.RawCell      -> (holder as RawCellViewHolder).bind(cell)
        }
    }

    // -------------------------------------------------------------------------
    // List update
    // -------------------------------------------------------------------------

    /**
     * Replaces the current cell list with [newCells], dispatching minimal
     * RecyclerView updates via DiffUtil. Called from the Fragment on Success state.
     */
    fun submitList(newCells: List<CellUiModel>) {
        val diffResult = DiffUtil.calculateDiff(CellDiffCallback(cells, newCells))
        cells = newCells
        diffResult.dispatchUpdatesTo(this)
    }

    // -------------------------------------------------------------------------
    // DiffUtil callback
    // -------------------------------------------------------------------------

    private class CellDiffCallback(
        private val old: List<CellUiModel>,
        private val new: List<CellUiModel>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = old.size
        override fun getNewListSize() = new.size

        override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean =
            old[oldPos].id == new[newPos].id

        override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean =
            old[oldPos] == new[newPos]
    }
}
