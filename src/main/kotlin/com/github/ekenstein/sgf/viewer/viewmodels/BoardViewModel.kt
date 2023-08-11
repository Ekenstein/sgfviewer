package com.github.ekenstein.sgf.viewer.viewmodels

import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfException
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.asPointOrNull
import com.github.ekenstein.sgf.editor.SgfEditor
import com.github.ekenstein.sgf.editor.addStones
import com.github.ekenstein.sgf.editor.commit
import com.github.ekenstein.sgf.editor.extractBoard
import com.github.ekenstein.sgf.editor.goToNextNode
import com.github.ekenstein.sgf.editor.goToNextTree
import com.github.ekenstein.sgf.editor.goToPreviousNode
import com.github.ekenstein.sgf.editor.goToPreviousTree
import com.github.ekenstein.sgf.editor.goToPreviousTreeOrStay
import com.github.ekenstein.sgf.editor.nextToPlay
import com.github.ekenstein.sgf.editor.pass
import com.github.ekenstein.sgf.editor.placeStone
import com.github.ekenstein.sgf.editor.tryRepeatWhileNot
import com.github.ekenstein.sgf.utils.MoveResult
import com.github.ekenstein.sgf.utils.orElse
import com.github.ekenstein.sgf.utils.orStay
import com.github.ekenstein.sgf.viewer.Stone
import com.github.ekenstein.sgf.viewer.boardSize
import com.github.ekenstein.sgf.viewer.views.BoardView
import com.github.ekenstein.sgf.viewer.views.BoardViewModel
import mu.KotlinLogging.logger
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import kotlin.properties.Delegates

private val logger = logger { }

class DefaultBoardViewModel : BoardViewModel {
    private val propertyChangeSupport = PropertyChangeSupport(this)

    var editor: SgfEditor by Delegates.observable(SgfEditor()) { property, oldValue, newValue ->
        propertyChangeSupport.firePropertyChange(property.name, oldValue, newValue)
        stones = editor.extractBoard().stones.map { (point, color) -> Stone(point, color) }.toSet()
    }

    private val boardSize
        get() = editor.boardSize()

    override val width
        get() = boardSize.first

    override val height
        get() = boardSize.second

    override var stones: Set<Stone> by Delegates.observable(emptySet()) { property, oldValue, newValue ->
        propertyChangeSupport.firePropertyChange(property.name, oldValue, newValue)
    }

    override fun addStone(color: SgfColor, x: Int, y: Int) {
        editor = editor.addStones(color, SgfPoint(x, y))
    }

    override fun placeStone(x: Int, y: Int) {
        try {
            editor = editor.placeStone(editor.nextToPlay(), x, y)
        } catch (ex: SgfException) {
            logger.catching(ex)
        }
    }

    override fun goToMove(x: Int, y: Int) {
        val point = SgfPoint(x, y)
        fun containsMove(editor: SgfEditor): Boolean {
            val p = editor.currentSequence.focus.properties.mapNotNull {
                when (it) {
                    is SgfProperty.Move.B -> it.move.asPointOrNull
                    is SgfProperty.Move.W -> it.move.asPointOrNull
                    else -> null
                }
            }.singleOrNull()

            return p == point
        }

        fun SgfEditor.goBackwards() = tryRepeatWhileNot(::containsMove) {
            it.goToPreviousNode()
        }

        fun SgfEditor.goForward() = tryRepeatWhileNot(::containsMove) {
            it.goToNextNode()
        }

        editor = editor.goBackwards().orElse { it.goForward() }.orStay()
    }

    override fun goRight() {
        editor = editor.goToNextTree().orStay()
    }

    override fun goLeft() {
        editor = editor.goToPreviousTree().orStay()
    }

    override fun pass() {
        try {
            editor = editor.pass(editor.nextToPlay())
        } catch (ex: SgfException) {
            logger.catching(ex)
        }
    }

    override fun prev() {
        editor = editor.goToPreviousNode().orStay()
    }

    override fun next() {
        editor = editor.goToNextNode().orStay()
    }

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        propertyChangeSupport.addPropertyChangeListener(listener)
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        propertyChangeSupport.removePropertyChangeListener(listener)
    }
}