package com.github.ekenstein.sgf.viewer.views

import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.editor.SgfEditor
import com.github.ekenstein.sgf.utils.MoveResult
import com.github.ekenstein.sgf.utils.TreeZipper
import com.github.ekenstein.sgf.utils.goDownLeft
import com.github.ekenstein.sgf.utils.goRight
import com.github.ekenstein.sgf.utils.goToRoot
import com.github.ekenstein.sgf.utils.goUp
import com.github.ekenstein.sgf.viewer.COLOR_BACKGROUND
import com.github.ekenstein.sgf.viewer.HasPropertyChangeSupport
import com.github.ekenstein.sgf.viewer.Nes
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.geom.Line2D
import java.beans.PropertyChangeListener
import javax.swing.JPanel

interface TreeViewModel : HasPropertyChangeSupport {
    val editor: SgfEditor
    val tree: List<Node>
}

data class Node(
    val color: SgfColor?,
    val focus: Boolean,
    val moveNumber: Int?,
    val children: List<List<Node>>
)

class TreeView(private val viewModel: TreeViewModel) : JPanel() {
    private val propertyChangeListener = PropertyChangeListener {
        repaint()
    }

    init {
        background = COLOR_BACKGROUND
        viewModel.addPropertyChangeListener(propertyChangeListener)
    }

    private fun getTheme() = Nes(600, 600, 19, 19)

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        check(g is Graphics2D) {
            "Blarf"
        }

        val theme = getTheme()
        drawTrees(g, theme, viewModel.tree, theme.stoneWidth, theme.stoneHeight)
//        drawTrees(
//            g,
//            viewModel.editor.currentTree.goToRoot(),
//            theme,
//            theme.stoneWidth,
//            theme.stoneHeight
//        )

        val gx = (viewModel.editor.currentTree.horizontalDepth(0) * theme.stoneWidth) + theme.stoneWidth
        val yOffset = (viewModel.editor.currentTree.verticalDepth(0) * theme.stoneHeight) + theme.stoneHeight
        val gy = yOffset + ((viewModel.editor.currentSequence.left.size) * theme.stoneHeight)

        theme.drawMarker(g, gx, gy)
    }

    private fun drawTrees(g: Graphics2D, theme: Nes, nodes: List<Node>, xOffset: Float, yOffset: Float) {
        nodes.forEachIndexed { index, node ->
            val gy = yOffset + index * theme.stoneHeight
            if (node.color != null) {
                theme.drawStone(g, xOffset, gy, node.color, COLOR_BACKGROUND)
            }

            node.children.forEach {
                drawTrees(g, theme, it, xOffset + theme.stoneWidth, gy)
            }
        }
    }

//    private fun drawTrees(
//        g: Graphics2D,
//        tree: TreeZipper<SgfGameTree>,
//        theme: Nes,
//        xOffset: Float,
//        yOffset: Float
//    ): Float {
//        drawSequence(g, theme, xOffset, yOffset, tree.focus.sequence.map { getColorFromNode(it) })
//        val gx = when (val result = tree.goDownLeft()) {
//            is MoveResult.Failure -> xOffset
//            is MoveResult.Success -> {
//                val gy = (tree.focus.sequence.size * theme.stoneHeight) + yOffset
//                drawTrees(g, result.position, theme, xOffset, gy)
//            }
//        }
//
//        return when (val result = tree.goRight()) {
//            is MoveResult.Failure -> gx
//            is MoveResult.Success -> drawTrees(g, result.position, theme, gx + theme.stoneWidth, yOffset)
//        }
//    }

    private tailrec fun <T> TreeZipper<T>.horizontalDepth(result: Int): Int = when (val tree = goUp()) {
        is MoveResult.Failure -> result
        is MoveResult.Success -> tree.position.horizontalDepth(
            result + left.size
        )
    }

    private tailrec fun TreeZipper<SgfGameTree>.verticalDepth(result: Int): Int = when (val tree = goUp()) {
        is MoveResult.Failure -> result
        is MoveResult.Success -> tree.position.verticalDepth(result + tree.position.focus.sequence.size)
    }

    private fun drawSequence(g: Graphics2D, theme: Nes, gx: Float, yOffset: Float, sequence: List<SgfColor?>) {
        sequence.forEachIndexed { index, color ->
            val gy = yOffset + ((index * 2) * theme.stoneHeight)
            if (color != null) {
                theme.drawStone(g, gx, gy, color, COLOR_BACKGROUND)
            }
        }
    }

    private fun getColorFromNode(node: SgfNode) = node.properties.mapNotNull {
        when (it) {
            is SgfProperty.Move.B -> SgfColor.Black
            is SgfProperty.Move.W -> SgfColor.White
            else -> null
        }
    }.singleOrNull()
}
