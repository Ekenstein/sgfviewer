package com.github.ekenstein.sgf.viewer

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default

class Options(parser: ArgParser) {
    val width by parser.option(
        type = ArgType.Int,
        fullName = "width",
        shortName = "w"
    ).default(1000)

    val height by parser.option(
        type = ArgType.Int,
        fullName = "height",
        shortName = "h"
    ).default(1000)
}
