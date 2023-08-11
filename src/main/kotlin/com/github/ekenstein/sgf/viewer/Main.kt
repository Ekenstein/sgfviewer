package com.github.ekenstein.sgf.viewer

import kotlinx.cli.ArgParser
import java.awt.EventQueue

private val parser = ArgParser("viewer", useDefaultHelpShortName = false)

fun main(args: Array<String>) {
    val options = Options(parser)
    parser.parse(args)

    EventQueue.invokeLater {
        val window = Window(options)
        window.isVisible = true
    }
}