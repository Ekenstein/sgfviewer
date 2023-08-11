package com.github.ekenstein.sgf.viewer.views

import com.github.ekenstein.sgf.viewer.Nes
import java.awt.Component
import kotlin.math.min

fun Component.getTheme(boardWidth: Int, boardHeight: Int): Nes {
    val width = min(width, 1000)
    val height = min(height, 1000)

    val size = Integer.max(500, min(width, height))
    return Nes(size, size, boardWidth, boardHeight)
}
