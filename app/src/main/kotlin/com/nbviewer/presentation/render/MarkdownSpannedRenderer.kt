package com.nbviewer.presentation.render

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.QuoteSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import com.nbviewer.R
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak

class MarkdownSpannedRenderer(private val context: Context) {

    private val bodyTextColor  by lazy { context.getColor(R.color.nb_text_body) }
    private val inlineCodeColor by lazy { context.getColor(R.color.nb_inline_code_fg) }
    private val quoteBarColor  by lazy { context.getColor(R.color.nb_quote_bar) }
    private val headingColor   by lazy { context.getColor(R.color.nb_heading_color) }

    fun render(document: Document): Spanned {
        val sb = SpannableStringBuilder()
        renderChildren(document, sb, listDepth = 0, orderedStart = null)
        return sb.trimEnd()
    }

    // ── Node dispatch ──────────────────────────────────────────────────────────

    private fun renderNode(
        node: Node,
        sb: SpannableStringBuilder,
        listDepth: Int,
        orderedStart: Int?
    ) {
        when (node) {
            is Document          -> renderChildren(node, sb, listDepth, orderedStart)
            is Heading           -> renderHeading(node, sb)
            is Paragraph         -> renderParagraph(node, sb, listDepth)
            is BulletList        -> renderBulletList(node, sb, listDepth)
            is OrderedList       -> renderOrderedList(node, sb, listDepth)
            is ListItem          -> renderListItem(node, sb, listDepth, orderedStart)
            is BlockQuote        -> renderBlockQuote(node, sb)
            is FencedCodeBlock   -> renderFencedCode(node, sb)
            is IndentedCodeBlock -> renderIndentedCode(node, sb)
            is ThematicBreak     -> {
                val start = sb.length
                sb.append("\n───────────────\n\n")
                sb.setSpan(
                    ForegroundColorSpan(bodyTextColor),
                    start, sb.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            is StrongEmphasis    -> renderInlineStyled(node, sb, listDepth, StyleSpan(Typeface.BOLD))
            is Emphasis          -> renderInlineStyled(node, sb, listDepth, StyleSpan(Typeface.ITALIC))
            is Code              -> renderInlineCode(node, sb)
            is Text              -> {
                val start = sb.length
                sb.append(node.literal)
                sb.setSpan(
                    ForegroundColorSpan(bodyTextColor),
                    start, sb.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            is SoftLineBreak     -> sb.append("\n")
            is HardLineBreak     -> sb.append("\n")
            else                 -> renderChildren(node, sb, listDepth, orderedStart)
        }
    }

    private fun renderChildren(
        node: Node,
        sb: SpannableStringBuilder,
        listDepth: Int,
        orderedStart: Int?
    ) {
        var child = node.firstChild
        while (child != null) {
            renderNode(child, sb, listDepth, orderedStart)
            child = child.next
        }
    }

    // ── Block elements ─────────────────────────────────────────────────────────

    private fun renderHeading(node: Heading, sb: SpannableStringBuilder) {
        if (sb.isNotEmpty()) sb.append("\n\n")
        val start = sb.length
        renderChildren(node, sb, listDepth = 0, orderedStart = null)
        val end = sb.length

        val (sizeMult, style) = when (node.level) {
            1    -> 1.6f to Typeface.BOLD
            2    -> 1.4f to Typeface.BOLD
            3    -> 1.2f to Typeface.BOLD
            else -> 1.0f to Typeface.BOLD
        }

        sb.setSpan(RelativeSizeSpan(sizeMult),         start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(StyleSpan(style),                   start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(ForegroundColorSpan(headingColor),  start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.append("\n\n")
    }

    private fun renderParagraph(
        node: Paragraph,
        sb: SpannableStringBuilder,
        listDepth: Int
    ) {
        // Body text color is applied at the Text node level — no span needed here
        renderChildren(node, sb, listDepth, orderedStart = null)
        sb.append("\n\n")
    }

    private fun renderBulletList(
        node: BulletList,
        sb: SpannableStringBuilder,
        listDepth: Int
    ) {
        var child = node.firstChild
        while (child != null) {
            if (child is ListItem) renderListItem(child, sb, listDepth + 1, orderedStart = null)
            child = child.next
        }
        if (listDepth == 0) sb.append("\n")
    }

    private fun renderOrderedList(
        node: OrderedList,
        sb: SpannableStringBuilder,
        listDepth: Int
    ) {
        var counter = node.startNumber
        var child = node.firstChild
        while (child != null) {
            if (child is ListItem) renderListItem(child, sb, listDepth + 1, orderedStart = counter++)
            child = child.next
        }
        if (listDepth == 0) sb.append("\n")
    }

    private fun renderListItem(
        node: ListItem,
        sb: SpannableStringBuilder,
        listDepth: Int,
        orderedStart: Int?
    ) {
        val indent = 16.dp * listDepth
        val prefix = if (orderedStart != null) "$orderedStart. " else "• "

        val start = sb.length

        // Prefix in body color
        val prefixStart = sb.length
        sb.append(prefix)
        sb.setSpan(
            ForegroundColorSpan(bodyTextColor),
            prefixStart, sb.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        renderChildren(node, sb, listDepth, orderedStart = null)

        if (sb.isNotEmpty() && sb.last() != '\n') sb.append("\n")
        sb.append("\n")

        sb.setSpan(
            LeadingMarginSpan.Standard(indent, indent + 12.dp),
            start, sb.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        if (listDepth > 1) {
            sb.setSpan(
                RelativeSizeSpan(0.95f),
                start, sb.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun renderBlockQuote(node: BlockQuote, sb: SpannableStringBuilder) {
        if (sb.isNotEmpty()) sb.append("\n")
        val start = sb.length
        renderChildren(node, sb, listDepth = 0, orderedStart = null)
        sb.setSpan(QuoteSpan(quoteBarColor), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.append("\n")
    }

    private fun renderFencedCode(node: FencedCodeBlock, sb: SpannableStringBuilder) {
        if (sb.isNotEmpty()) sb.append("\n")
        val start = sb.length
        sb.append(node.literal.trimEnd())
        val end = sb.length
        sb.setSpan(TypefaceSpan("monospace"),            start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(ForegroundColorSpan(inlineCodeColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(LeadingMarginSpan.Standard(8.dp),     start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.append("\n\n")
    }

    private fun renderIndentedCode(node: IndentedCodeBlock, sb: SpannableStringBuilder) {
        if (sb.isNotEmpty()) sb.append("\n")
        val start = sb.length
        sb.append(node.literal.trimEnd())
        val end = sb.length
        sb.setSpan(TypefaceSpan("monospace"),            start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(ForegroundColorSpan(inlineCodeColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(LeadingMarginSpan.Standard(8.dp),     start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.append("\n\n")
    }

    // ── Inline elements ────────────────────────────────────────────────────────

    private fun renderInlineStyled(
        node: Node,
        sb: SpannableStringBuilder,
        listDepth: Int,
        vararg spans: Any
    ) {
        val start = sb.length
        renderChildren(node, sb, listDepth, orderedStart = null)
        spans.forEach { span ->
            sb.setSpan(span, start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun renderInlineCode(node: Code, sb: SpannableStringBuilder) {
        val start = sb.length
        sb.append(node.literal)
        sb.setSpan(TypefaceSpan("monospace"),            start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(ForegroundColorSpan(inlineCodeColor), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private val Int.dp: Int
        get() = (this * context.resources.displayMetrics.density).toInt()

    private fun SpannableStringBuilder.trimEnd(): SpannableStringBuilder {
        var end = length
        while (end > 0 && this[end - 1] == '\n') end--
        return SpannableStringBuilder(subSequence(0, end))
    }
}