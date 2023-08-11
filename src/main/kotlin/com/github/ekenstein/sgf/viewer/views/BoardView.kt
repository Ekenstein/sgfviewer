package com.github.ekenstein.sgf.viewer.views

import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.viewer.COLOR_BACKGROUND
import com.github.ekenstein.sgf.viewer.HasPropertyChangeSupport
import com.github.ekenstein.sgf.viewer.Stone
import mu.KotlinLogging.logger
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.beans.PropertyChangeListener
import javax.swing.AbstractAction
import javax.swing.JPanel
import javax.swing.KeyStroke

private val logger = logger { }
private const val GO_LEFT_ACTION = "Prev"
private const val GO_RIGHT_ACTION = "Next"
private const val PASS_ACTION = "Pass"
private const val GO_UP_ACTION = "Up"
private const val GO_DOWN_ACTION = "Down"

interface BoardViewModel : HasPropertyChangeSupport {
    val width: Int
    val height: Int
    val stones: Set<Stone>

    fun addStone(color: SgfColor, x: Int, y: Int)
    fun placeStone(x: Int, y: Int)
    fun goToMove(x: Int, y: Int)
    fun goRight()
    fun goLeft()
    fun pass()
    fun prev()
    fun next()
}

class BoardView(private val viewModel: BoardViewModel) : JPanel() {
    private fun getTheme() = getTheme(viewModel.width, viewModel.height)

    private val goUpAction = object : AbstractAction(GO_UP_ACTION) {
        override fun actionPerformed(e: ActionEvent?) {
            viewModel.prev()
        }
    }

    private val goDownAction = object : AbstractAction(GO_DOWN_ACTION) {
        override fun actionPerformed(e: ActionEvent?) {
            viewModel.next()
        }

    }

    private val goLeftAction = object : AbstractAction(GO_LEFT_ACTION) {
        override fun actionPerformed(e: ActionEvent) {
            viewModel.goLeft()
        }
    }

    private val goRightAction = object : AbstractAction(GO_RIGHT_ACTION) {
        override fun actionPerformed(e: ActionEvent?) {
            viewModel.goRight()
        }
    }

    private val passAction = object : AbstractAction(PASS_ACTION) {
        override fun actionPerformed(e: ActionEvent?) {
            viewModel.pass()
        }
    }

    private val mouseListener = object : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) {
            super.mousePressed(e)
            val theme = getTheme()
            val x = theme.convertGxToPlayAreaX(e.x)
            val y = theme.convertGyToPlayAreaY(e.y)

            if (x != null && y != null) {
                if ((e.modifiersEx and InputEvent.SHIFT_DOWN_MASK) != 0) {
                    viewModel.goToMove(x, y)
                } else if ((e.modifiersEx and InputEvent.CTRL_DOWN_MASK) != 0) {
                    viewModel.addStone(SgfColor.Black, x, y)
                } else {
                    viewModel.placeStone(x, y)
                }
            }
        }

        override fun mouseWheelMoved(e: MouseWheelEvent) {
            if (e.wheelRotation < 0) {
                viewModel.next()
            } else if (e.wheelRotation > 0) {
                viewModel.prev()
            }
        }

        override fun mouseMoved(e: MouseEvent) {
            super.mouseMoved(e)
            val theme = getTheme()
            val x = theme.convertGxToPlayAreaX(e.x)
            val y = theme.convertGyToPlayAreaY(e.y)

            if (x != null && y != null && mousePosition != x to y) {
                mousePosition = x to y
                repaint()
            }
        }
    }

    private val propertyChangeListener = PropertyChangeListener {
        repaint()
    }

    init {
        background = COLOR_BACKGROUND
        addMouseListener(mouseListener)
        addMouseMotionListener(mouseListener)
        addMouseWheelListener(mouseListener)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), GO_LEFT_ACTION)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), GO_RIGHT_ACTION)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), PASS_ACTION)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), GO_UP_ACTION)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), GO_DOWN_ACTION)

        actionMap.put(GO_LEFT_ACTION, goLeftAction)
        actionMap.put(GO_RIGHT_ACTION, goRightAction)
        actionMap.put(PASS_ACTION, passAction)
        actionMap.put(GO_UP_ACTION, goUpAction)
        actionMap.put(GO_DOWN_ACTION, goDownAction)

        viewModel.addPropertyChangeListener(propertyChangeListener)
    }

    private var mousePosition: Pair<Int, Int>? = null

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        check(g is Graphics2D) {
            "The graphics object is not a Graphics2D implementation."
        }

        val theme = getTheme()

        theme.drawEmptyBoard(g)
        viewModel.stones.forEach {
            theme.drawStone(g, it)
        }

        mousePosition?.let { (x, y) -> theme.drawMarker(g, x, y) }
    }
}
