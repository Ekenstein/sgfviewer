package com.github.ekenstein.sgf.viewer

import com.github.ekenstein.sgf.utils.TreeZipper
import com.github.ekenstein.sgf.utils.Zipper

fun <T> TreeZipper<T>.toZipper() = Zipper(left, focus, right)