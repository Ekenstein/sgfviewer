package com.github.ekenstein.sgf.viewer

import com.github.ekenstein.sgf.SgfCollection
import com.github.ekenstein.sgf.editor.SgfEditor
import com.github.ekenstein.sgf.editor.commit
import com.github.ekenstein.sgf.parser.from
import com.github.ekenstein.sgf.serialization.encode
import com.github.ekenstein.sgf.viewer.viewmodels.DefaultBoardViewModel
import com.github.ekenstein.sgf.viewer.viewmodels.DefaultTreeViewModel
import com.github.ekenstein.sgf.viewer.views.BoardView
import com.github.ekenstein.sgf.viewer.views.TreeView
import com.github.ekenstein.sgf.viewer.views.TreeViewModel
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.event.KeyEvent
import java.beans.PropertyChangeListener
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.JScrollPane
import javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
import javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
import javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED

class Window(options: Options) : JFrame() {
    init {
        title = "viewer"
        defaultCloseOperation = EXIT_ON_CLOSE
        setLocationRelativeTo(null)
        extendedState = MAXIMIZED_BOTH
        layout = GridLayout()

        val treeViewModel = DefaultTreeViewModel()
        val boardViewModel = DefaultBoardViewModel().apply {
            addPropertyChangeListener(
                PropertyChangeListener {
                    val editor = it.newValue as? SgfEditor
                    if (editor != null) {
                        treeViewModel.editor = editor
                    }
                }
            )
        }

        val boardView = BoardView(boardViewModel).apply {
            preferredSize = Dimension(1000, 1000)
            isVisible = true

        }

        val treeView = TreeView(treeViewModel).apply {
            autoscrolls = true
        }
        val scrollPane = JScrollPane(treeView, VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_AS_NEEDED).apply {
            preferredSize = Dimension(300, 600)
        }

        add(boardView)
        add(scrollPane)

        val menuBar = JMenuBar().apply {
            val fileMenu = JMenu("File").apply {
                mnemonic = KeyEvent.VK_F

                val open = JMenuItem("Open file").apply {
                    mnemonic = KeyEvent.VK_O
                    addActionListener {
                        JFileChooser().apply {
                            when (showOpenDialog(this@Window)) {
                                JFileChooser.APPROVE_OPTION -> {
                                    val collection = SgfCollection.from(selectedFile.toPath()) {
                                        preserveMalformedProperties = true
                                    }

                                    val editor = SgfEditor(collection.trees.head)
                                    boardViewModel.editor = editor
                                }
                            }
                        }
                    }
                }

                val save = JMenuItem("Save as").apply {
                    mnemonic = KeyEvent.VK_S
                    addActionListener {
                        JFileChooser().apply {
                            when (showSaveDialog(this@Window)) {
                                JFileChooser.APPROVE_OPTION -> {
                                    val collection = boardViewModel.editor.commit()
                                    selectedFile.outputStream().use {
                                        collection.encode(it)
                                    }
                                }
                            }
                        }
                    }
                }

                val newGame = JMenuItem("New game").apply {
                    addActionListener {
                        boardViewModel.editor = SgfEditor()
                    }
                }

                add(newGame)
                add(open)
                add(save)
            }

            add(fileMenu)
        }

        jMenuBar = menuBar
    }
}