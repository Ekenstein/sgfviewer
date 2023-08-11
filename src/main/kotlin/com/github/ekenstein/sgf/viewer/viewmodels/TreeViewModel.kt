package com.github.ekenstein.sgf.viewer.viewmodels

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
import com.github.ekenstein.sgf.utils.isSuccess
import com.github.ekenstein.sgf.viewer.views.Node
import com.github.ekenstein.sgf.viewer.views.TreeViewModel
import mu.KotlinLogging.logger
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import kotlin.properties.Delegates

private val logger = logger { }

class DefaultTreeViewModel : TreeViewModel {
    private val propertyChangeSupport = PropertyChangeSupport(this)

    override var editor: SgfEditor by Delegates.observable(SgfEditor()) { property, oldValue, newValue ->
        propertyChangeSupport.firePropertyChange(property.name, oldValue, newValue)
        updateTree()
    }
    override var tree: List<Node> by Delegates.observable(emptyList()) { property, oldValue, newValue ->
        propertyChangeSupport.firePropertyChange(property.name, oldValue, newValue)
    }

    private fun updateTree() {
        fun inner(tree: TreeZipper<SgfGameTree>, result: List<Node>): List<Node> {
            val sequence = tree.focus.sequence.map {
                Node(
                    color = colorFromNode(it),
                    focus = false,
                    moveNumber = null,
                    children = emptyList()
                )
            }

            val continuation = when (val child = tree.goDownLeft()) {
                is MoveResult.Failure -> emptyList()
                is MoveResult.Success -> inner(child.position, emptyList())
            }

            val children = mutableListOf<List<Node>>()
            var next = tree.goRight()
            while (next.isSuccess()) {
                children.add(inner(next.position, emptyList()))
                next = next.position.goRight()
            }

            val last = sequence.last().copy(
                children = children
            )

            return result + sequence.take(sequence.size - 1) + last + continuation
        }

        val nodes = inner(editor.currentTree.goToRoot(), emptyList())
        tree = nodes
    }

    private fun colorFromNode(node: SgfNode) = node.properties.mapNotNull {
        when (it) {
            is SgfProperty.Move.B -> SgfColor.Black
            is SgfProperty.Move.W -> SgfColor.White
            else -> null
        }
    }.singleOrNull()

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        propertyChangeSupport.addPropertyChangeListener(listener)
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        propertyChangeSupport.removePropertyChangeListener(listener)
    }
}