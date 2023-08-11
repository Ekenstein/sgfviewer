package com.github.ekenstein.sgf.viewer

import com.github.ekenstein.sgf.Move
import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.editor.SgfEditor
import com.github.ekenstein.sgf.editor.goToPreviousNode
import com.github.ekenstein.sgf.editor.goToRootNode
import com.github.ekenstein.sgf.utils.MoveResult
import com.github.ekenstein.sgf.utils.NonEmptySet
import com.github.ekenstein.sgf.utils.nonEmptySetOf
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Line2D
import java.awt.geom.Rectangle2D
import java.lang.Float.max
import java.lang.Float.min
import kotlin.math.ceil
import kotlin.math.roundToInt

private val COLOR_SHADOW = Color.BLACK
private val COLOR_GOBAN = Color(162, 92, 0, 255)
val COLOR_BACKGROUND = Color(0, 79, 92, 255)

private const val PIXEL_SIZE = 5F
private const val PILLAR_HEIGHT = 7 * PIXEL_SIZE
private const val BOARD_THICKNESS_FACTOR = 0.13F
private const val PERSPECTIVE_FACTOR = 0.01F
private const val BOARD_SCALE = 0.90F
private const val SHADOW_OFFSET_FACTOR = 0.7F
private const val PLAY_AREA_OFFSET_FACTOR = 0.03F
private const val STONE_WIDTH = 7 * PIXEL_SIZE
private const val STONE_HEIGHT = 8 * PIXEL_SIZE
private val COLOR_MARKER = Color(212, 240, 160, 255)
private val COLOR_MARKER_SHADOW = Color(86, 93, 24, 255)

data class Stone(val point: SgfPoint, val color: SgfColor)

fun SgfEditor.boardSize() = goToRootNode().currentNode.property<SgfProperty.Root.SZ>()?.let {
    it.width to it.height
} ?: (19 to 19)

fun SgfEditor.getMoves(): List<Stone> {
    tailrec fun SgfEditor.next(result: List<Stone>): List<Stone> {
        val stones = currentNode.properties.mapNotNull { property ->
            when (property) {
                is SgfProperty.Move.B -> when (val move = property.move) {
                    Move.Pass -> null
                    is Move.Stone -> Stone(move.point, SgfColor.Black)
                }
                is SgfProperty.Move.W -> when (val move = property.move) {
                    Move.Pass -> null
                    is Move.Stone -> Stone(move.point, SgfColor.White)
                }
                else -> null
            }
        }

        val allStones = result + stones

        return when (val next = goToPreviousNode()) {
            is MoveResult.Failure -> allStones
            is MoveResult.Success -> next.position.next(allStones)
        }
    }

    return next(emptyList())
}

fun starPoints(boardSize: Int): Set<SgfPoint> {
    val edgeDistance = edgeDistance(boardSize)
        ?: return emptySet()
    val middle = ceil(boardSize / 2.0).toInt()
    val tengen = SgfPoint(middle, middle)

    fun points(handicap: Int): NonEmptySet<SgfPoint> = when (handicap) {
        2 -> nonEmptySetOf(
            SgfPoint(x = edgeDistance, y = boardSize - edgeDistance + 1),
            SgfPoint(x = boardSize - edgeDistance + 1, y = edgeDistance)
        )
        3 -> nonEmptySetOf(SgfPoint(x = boardSize - edgeDistance + 1, y = boardSize - edgeDistance + 1)) + points(2)
        4 -> nonEmptySetOf(SgfPoint(x = edgeDistance, y = edgeDistance)) + points(3)
        5 -> nonEmptySetOf(tengen) + points(4)
        6 -> nonEmptySetOf(
            SgfPoint(x = edgeDistance, y = middle),
            SgfPoint(x = boardSize - edgeDistance + 1, y = middle)
        ) + points(4)
        7 -> nonEmptySetOf(tengen) + points(6)
        8 -> nonEmptySetOf(
            SgfPoint(middle, edgeDistance),
            SgfPoint(middle, boardSize - edgeDistance + 1)
        ) + points(6)
        9 -> nonEmptySetOf(tengen) + points(8)
        else -> error("Invalid handicap value $handicap")
    }

    return points(starPointsForBoardSize(boardSize))
}

private fun starPointsForBoardSize(boardSize: Int) = when {
    boardSize < 7 -> 0
    boardSize == 7 -> 4
    boardSize % 2 == 0 -> 4
    else -> 9
}

private fun edgeDistance(boardSize: Int) = when {
    boardSize < 7 -> null
    boardSize < 13 -> 3
    else -> 4
}

class Nes(
    private val width: Int,
    private val height: Int,
    private val boardWidth: Int,
    private val boardHeight: Int
) {
    private val gobanWidth = BOARD_SCALE * width
    private val gobanHeight = BOARD_SCALE * height
    private val gobanThickness = gobanHeight * BOARD_THICKNESS_FACTOR
    private val gobanPerspective = (gobanHeight + gobanThickness) * PERSPECTIVE_FACTOR

    private val playAreaHeight = gobanHeight - gobanThickness - gobanPerspective - PILLAR_HEIGHT
    private val playAreaOffsetX = gobanWidth * PLAY_AREA_OFFSET_FACTOR
    private val playAreaOffsetY = playAreaHeight * PLAY_AREA_OFFSET_FACTOR

    private val gobanStartX = (width / 2) - (gobanWidth / 2)
    private val gobanStartY = (height / 2) - ((gobanHeight - gobanPerspective) / 2)

    private val intersectionHeight = (playAreaHeight - 2 * playAreaOffsetY) / (boardHeight - 1).toFloat()
    private val intersectionWidth = (gobanWidth - 2 * playAreaOffsetX) / (boardWidth - 1).toFloat()

    private val playAreaStartX = gobanStartX + playAreaOffsetX
    private val playAreaStartY = gobanStartY + playAreaOffsetY

    private val stoneWidthPixels = 7F
    private val stoneHeightPixels = 8F

    private val pixelWidth = (intersectionWidth / stoneWidthPixels) * 0.90F
    val pixelHeight = (intersectionHeight / stoneHeightPixels) * 0.90F

    val stoneWidth = stoneWidthPixels * pixelWidth
    val stoneHeight = stoneHeightPixels * pixelHeight

    fun convertGxToPlayAreaX(gx: Int): Int? {
        val offset = playAreaX(1)
        val x = (((gx - offset) / intersectionWidth) + 1).roundToInt()

        return x.takeIf { it in (1..boardWidth) }
    }

    fun convertGyToPlayAreaY(gy: Int): Int? {
        val offset = playAreaY(1)
        val y = (((gy - offset) / intersectionHeight) + 1).roundToInt()
        return y.takeIf { it in (1..boardHeight) }
    }

    private fun playAreaX(x: Int): Float {
        return playAreaStartX + intersectionWidth * (x - 1)
    }

    private fun playAreaY(y: Int): Float {
        return playAreaStartY + intersectionHeight * (y - 1)
    }

    fun drawEmptyBoard(g: Graphics2D) {
        g.color = COLOR_BACKGROUND
        g.fillRect(0, 0, width, height)

        g.color = COLOR_SHADOW
        val shadow = Rectangle2D.Float(
            gobanStartX + (gobanStartX * SHADOW_OFFSET_FACTOR),
            gobanStartY + (gobanStartY * SHADOW_OFFSET_FACTOR),
            gobanWidth,
            gobanHeight
        )
        g.fill(shadow)

        g.color = COLOR_GOBAN
        val playArea = Rectangle2D.Float(
            gobanStartX,
            gobanStartY,
            gobanWidth,
            gobanHeight - gobanThickness - gobanPerspective - PILLAR_HEIGHT
        )
        g.fill(playArea)

        drawBoardThickness(g)
        drawPillars(g)
        drawIntersections(g)
        drawStarPoints(g)
    }

    private fun drawBoardThickness(g: Graphics2D) {
        val startX = gobanStartX
        val startY = gobanStartY + playAreaHeight
        val shadowWidth = (gobanWidth / PIXEL_SIZE).toInt()
        val shadowHeight = (gobanThickness / PIXEL_SIZE).toInt()

        g.color = COLOR_SHADOW
        val shadow = Rectangle2D.Float(startX, startY, shadowWidth * PIXEL_SIZE, shadowHeight * PIXEL_SIZE)
        g.fill(shadow)

        repeat(shadowHeight) { y ->
            val gy = startY + PIXEL_SIZE * y
            val x = if (y % 2 == 0) {
                startX + PIXEL_SIZE
            } else {
                startX
            }
            g.drawShadowedLine(x, gy, shadowWidth)
        }
    }

    private fun drawIntersections(g: Graphics2D) {
        g.stroke = BasicStroke(PIXEL_SIZE)
        g.color = COLOR_SHADOW

        repeat(boardWidth) { x ->
            val gx = playAreaStartX + intersectionWidth * x
            val startY = playAreaStartY
            val endY = playAreaY(boardHeight)
            val line = Line2D.Float(
                gx,
                startY,
                gx,
                endY
            )

            g.draw(line)
        }

        repeat(boardHeight) { y ->
            val gy = playAreaStartY + intersectionHeight * y
            val startX = playAreaStartX
            val endX = playAreaX(boardWidth)
            val line = Line2D.Float(
                startX,
                gy,
                endX,
                gy
            )

            g.draw(line)
        }
    }

    private fun drawStarPoints(g: Graphics2D) {
        if (boardHeight != boardWidth) {
            return
        }

        g.color = COLOR_SHADOW

        val starPoints = starPoints(boardWidth)
        starPoints.forEach { (x, y) ->
            drawStarPoint(g, x, y)
        }
    }

    private fun drawStarPoint(g: Graphics2D, x: Int, y: Int) {
        val gx = playAreaX(x)
        val gy = playAreaY(y)

        val gh = pixelHeight * 3
        val gw = pixelWidth * 4

        val topLeftX = gx - (gw / 2)
        val topLeftY = gy - (gh / 2)

        val rect = Rectangle2D.Float(topLeftX, topLeftY, gw, gh)
        g.fill(rect)
    }

    private fun drawPillars(g: Graphics2D) {
        val pillarWidthInPixels = 15
        val gobanWidthInPixels = (gobanWidth / PIXEL_SIZE).toInt()

        val middleXInPixels = gobanWidthInPixels / 2

        val leftPillarMiddleXInPixels = middleXInPixels - gobanWidthInPixels / 3
        val leftPillarStartXInPixels = leftPillarMiddleXInPixels - (pillarWidthInPixels / 2) + 1

        drawPillar(g, this.gobanStartX + leftPillarStartXInPixels * PIXEL_SIZE)

        val rightPillarMiddleXInPixels = gobanWidthInPixels - leftPillarMiddleXInPixels
        val rightPillarStartXInPixels = rightPillarMiddleXInPixels - (pillarWidthInPixels / 2) + 3

        drawPillar(g, rightPillarStartXInPixels * PIXEL_SIZE)
    }

    private fun drawPillar(g: Graphics2D, startX: Float) {
        val startY = (gobanStartY + playAreaHeight) + (gobanThickness / PIXEL_SIZE).toInt() * PIXEL_SIZE
        g.drawShadowedLine(startX + 2 * PIXEL_SIZE, startY, 11)
        g.drawShadowedLine(startX, startY + PIXEL_SIZE * 2, 15)
        g.drawShadowedLine(startX, startY + PIXEL_SIZE * 4, 15)
        g.drawShadowedLine(startX + 2 * PIXEL_SIZE, startY + PIXEL_SIZE * 6, 11)
    }

    fun drawStone(g: Graphics2D, stone: Stone) {
        val (x, y) = stone.point
        val gx = playAreaX(x)
        val gy = playAreaY(y)
        when (stone.color) {
            SgfColor.Black -> drawBlackStone(g, COLOR_GOBAN, gx, gy)
            SgfColor.White -> drawWhiteStone(g, COLOR_GOBAN, gx, gy)
        }
    }

    fun drawStone(g: Graphics2D, gx: Float, gy: Float, color: SgfColor, bgColor: Color) = when (color) {
        SgfColor.Black -> drawBlackStone(g, bgColor, gx, gy)
        SgfColor.White -> drawWhiteStone(g, bgColor, gx, gy)
    }

    fun clearPoint(g: Graphics2D, x: Int, y: Int) {
        val gx = playAreaX(x)
        val gy = playAreaY(y)
        val topLeftX = gx - (stoneWidth / 2)
        val topLeftY = gy - (stoneHeight / 2)

        val playAreaStopX = playAreaX(boardWidth)
        val playAreaStopY = playAreaY(boardHeight)

        g.color = COLOR_GOBAN
        val rect = Rectangle2D.Float(
            topLeftX,
            topLeftY,
            stoneWidth,
            stoneHeight
        )

        g.fill(rect)

        val starPoints = if (boardWidth == boardHeight) {
            starPoints(boardWidth)
        } else {
            emptySet()
        }

        if (SgfPoint(x, y) in starPoints) {
            drawStarPoint(g, x, y)
        }

        g.color = COLOR_SHADOW
        g.stroke = BasicStroke(PIXEL_SIZE)

        val l1 = Line2D.Float(
            gx,
            max(topLeftY, playAreaStartY),
            gx,
            min(topLeftY + stoneHeight, playAreaStopY)
        )
        g.draw(l1)

        val l2 = Line2D.Float(
            max(topLeftX, playAreaStartX),
            gy,
            min(topLeftX + stoneWidth, playAreaStopX),
            gy
        )

        g.draw(l2)
    }

    fun drawMarker(g: Graphics2D, gx: Float, gy: Float) {
        g.color = COLOR_MARKER
        val markerHeight = stoneHeight
        val markerWidth = stoneWidth

        val topLeftX = gx - (markerWidth / 2)
        val topLeftY = gy - (markerHeight / 2)
        val rightX = topLeftX + markerWidth - pixelWidth

        // top
        g.fill(Rectangle2D.Float(topLeftX, topLeftY, 3 * pixelWidth, pixelHeight))
        g.fill(Rectangle2D.Float(rightX - 2 * pixelWidth, topLeftY, 3 * pixelWidth, pixelHeight))
        g.fill(Rectangle2D.Float(topLeftX, topLeftY + pixelHeight, pixelWidth, pixelHeight))
        g.fill(Rectangle2D.Float(rightX, topLeftY + pixelHeight, pixelWidth, pixelHeight))

        // bottom
        val bottomY = topLeftY + markerHeight - pixelHeight
        g.fill(Rectangle2D.Float(topLeftX, bottomY, 3 * pixelWidth, pixelHeight))
        g.fill(Rectangle2D.Float(rightX - 2 * pixelWidth, bottomY, 3 * pixelWidth, pixelHeight))
        g.fill(Rectangle2D.Float(topLeftX, bottomY - pixelHeight, pixelWidth, pixelHeight))
        g.fill(Rectangle2D.Float(rightX, bottomY - pixelHeight, pixelWidth, pixelHeight))

        // middle
        val middleX = topLeftX + (markerWidth / 2) - pixelWidth
        val middleY = topLeftY + (markerHeight / 2) - pixelHeight

        g.fill(Rectangle2D.Float(middleX, middleY, 2 * pixelWidth, 2 * pixelHeight))

        g.color = COLOR_MARKER_SHADOW
        // shadow

        g.fill(Rectangle2D.Float(middleX, middleY + 2 * pixelHeight, 2 * pixelWidth, pixelHeight))

        g.fill(Rectangle2D.Float(topLeftX + pixelWidth, topLeftY + pixelHeight, 2 * pixelWidth, pixelHeight))
        g.fill(Rectangle2D.Float(rightX - 2 * pixelWidth, topLeftY + pixelHeight, 2 * pixelWidth, pixelHeight))
        g.fill(Rectangle2D.Float(topLeftX, topLeftY + 2 * pixelHeight, pixelWidth, pixelHeight))
        g.fill(Rectangle2D.Float(rightX, topLeftY + 2 * pixelHeight, pixelWidth, pixelHeight))

        g.fill(Rectangle2D.Float(topLeftX, bottomY + pixelHeight, pixelWidth * 3, pixelHeight))
        g.fill(Rectangle2D.Float(rightX - 2 * pixelWidth, bottomY + pixelHeight, pixelWidth * 3, pixelHeight))
    }

    fun drawMarker(g: Graphics2D, x: Int, y: Int) {
        val gx = playAreaX(x)
        val gy = playAreaY(y)

        drawMarker(g, gx, gy)
    }

    private fun drawWhiteStone(
        g: Graphics2D,
        bgColor: Color,
        gx: Float,
        gy: Float,
    ) {
        val topLeftX = gx - (stoneWidth / 2)
        val topLeftY = gy - (stoneHeight / 2)

        g.color = Color.WHITE
        val bg = Rectangle2D.Float(
            topLeftX,
            topLeftY,
            stoneWidth,
            stoneHeight
        )
        g.fill(bg)

        g.color = bgColor
        g.fill(
            Rectangle2D.Float(
                topLeftX,
                topLeftY,
                2 * pixelWidth,
                pixelHeight
            )
        )

        g.fill(
            Rectangle2D.Float(
                topLeftX + stoneWidth - 2 * pixelWidth,
                topLeftY,
                2 * pixelWidth,
                pixelHeight
            )
        )

        g.fill(
            Rectangle2D.Float(
                topLeftX,
                topLeftY + pixelHeight,
                pixelWidth,
                pixelHeight
            )
        )

        g.fill(
            Rectangle2D.Float(
                topLeftX + stoneWidth - pixelWidth,
                topLeftY + pixelHeight,
                pixelWidth,
                pixelHeight
            )
        )

        g.fill(
            Rectangle2D.Float(
                topLeftX,
                topLeftY + stoneHeight - pixelHeight,
                pixelWidth * 2,
                pixelHeight
            )
        )

        g.fill(
            Rectangle2D.Float(
                topLeftX,
                topLeftY + stoneHeight - 2 * pixelHeight,
                pixelWidth,
                pixelHeight
            )
        )
        g.fill(
            Rectangle2D.Float(
                topLeftX + stoneWidth - pixelWidth,
                topLeftY + stoneHeight - pixelHeight,
                pixelWidth,
                pixelHeight
            )
        )

        g.color = COLOR_SHADOW
        g.fill(
            Rectangle2D.Float(
                topLeftX + stoneWidth - pixelWidth,
                topLeftY + stoneHeight - 3 * pixelHeight,
                pixelWidth,
                pixelHeight * 2
            )
        )

        g.fill(
            Rectangle2D.Float(
                topLeftX + stoneWidth - 2 * pixelWidth,
                topLeftY + stoneHeight - 2 * pixelHeight,
                pixelWidth * 2,
                pixelHeight
            )
        )
        g.fill(
            Rectangle2D.Float(
                topLeftX + 2 * pixelWidth,
                topLeftY + stoneHeight - pixelHeight,
                pixelWidth * 4,
                pixelHeight
            )
        )
    }

    private fun drawBlackStone(
        g: Graphics2D,
        bgColor: Color,
        gx: Float,
        gy: Float
    ) {
        val topLeftX = gx - (stoneWidth / 2)
        val topLeftY = gy - (stoneHeight / 2)

        g.color = Color.BLACK
        val bg = Rectangle2D.Float(topLeftX, topLeftY, stoneWidth, stoneHeight)
        g.fill(bg)

        g.color = bgColor

        g.fill(
            Rectangle2D.Float(
                topLeftX,
                topLeftY,
                2 * pixelWidth,
                pixelHeight
            )
        )
        g.fill(
            Rectangle2D.Float(
                topLeftX + stoneWidth - (2 * pixelWidth),
                topLeftY,
                2 * pixelWidth,
                pixelHeight
            )
        )

        g.fill(
            Rectangle2D.Float(
                topLeftX,
                topLeftY + pixelHeight,
                pixelWidth,
                pixelHeight
            )
        )
        g.fill(
            Rectangle2D.Float(
                topLeftX + stoneWidth - pixelWidth,
                topLeftY + pixelHeight,
                pixelWidth,
                pixelHeight
            )
        )

        g.fill(
            Rectangle2D.Float(
                topLeftX,
                topLeftY + stoneHeight - pixelHeight,
                2 * pixelWidth,
                pixelHeight
            )
        )
        g.fill(
            Rectangle2D.Float(
                topLeftX + stoneWidth - (2 * pixelWidth),
                topLeftY + stoneHeight - pixelHeight,
                2 * pixelWidth,
                pixelHeight
            )
        )

        g.fill(
            Rectangle2D.Float(
                topLeftX,
                topLeftY + stoneHeight - 2 * pixelHeight,
                pixelWidth,
                pixelHeight
            )
        )
        g.fill(
            Rectangle2D.Float(
                topLeftX + stoneWidth - pixelWidth,
                topLeftY + stoneHeight - 2 * pixelHeight,
                pixelWidth,
                pixelHeight
            )
        )

        g.color = Color.WHITE
        g.fill(
            Rectangle2D.Float(
                topLeftX + pixelWidth * 2,
                topLeftY + pixelHeight,
                pixelWidth,
                pixelHeight
            )
        )
        g.fill(
            Rectangle2D.Float(
                topLeftX + pixelWidth,
                topLeftY + 2 * pixelHeight,
                pixelWidth,
                pixelHeight * 3
            )
        )
    }
}

private fun Graphics2D.drawShadowedLine(startX: Float, startY: Float, width: Int) {
    repeat(width) { x ->
        color = if (x % 2 == 0) {
            COLOR_GOBAN
        } else {
            COLOR_SHADOW
        }

        val gx = startX + (PIXEL_SIZE * x)
        val pixel = Rectangle2D.Float(gx, startY, PIXEL_SIZE, PIXEL_SIZE)
        fill(pixel)
    }
}
