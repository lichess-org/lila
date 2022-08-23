package lila.app
package templating

import shogi.{ Color, Pos, Situation }
import shogi.variant.Variant
import lila.api.Context

import lila.app.ui.ScalatagsTemplate._
import lila.game.Pov

trait ShogigroundHelper {

  private val sgBoard   = tag("sg-board")
  private val sgSquares = tag("sg-squares")
  private val sgPieces  = tag("sg-pieces")
  val sgWrapContent     = sgBoard(sgSquares)

  val sgHandTop    = div(cls := "sg-hand-wrap hand-top")(tag("sg-hand"))
  val sgHandBottom = div(cls := "sg-hand-wrap hand-bottom")(tag("sg-hand"))

  def shogiground(sit: Situation, orient: Color, lastMove: List[Pos] = Nil)(implicit ctx: Context): Frag =
    sgWrap(sit.variant, orient.some) {
      frag(
        sgSquares,
        sgPieces {
          raw {
            val scale = 50
            def x(p: Pos) =
              orient.fold(sit.variant.numberOfFiles - p.file.index - 1, p.file.index) * scale
            def y(p: Pos) =
              orient.fold(p.rank.index, sit.variant.numberOfRanks - p.rank.index - 1) * scale
            if (ctx.pref.isBlindfold) ""
            else
              sit.board.pieces.map { case (pos, piece) =>
                val klass = s"${piece.color.name} ${piece.role.name}"
                s"""<piece class="$klass" style="transform: translate(${x(pos)}%, ${y(
                    pos
                  )}%) scale(0.5)"></piece>"""
              } mkString ""
          }
        }
      )
    }

  def shogiground(pov: Pov)(implicit ctx: Context): Frag =
    shogiground(
      sit = pov.game.situation,
      orient = pov.color,
      lastMove = ~pov.game.history.lastMove.map(_.positions)
    )

  // if preload with the fake grid is not satisfactory, we will have to send all the <sq></sq>,
  // that would mean cca 8% increase of the document size...
  private def sgWrap(variant: Variant, orient: Option[Color])(content: Frag): Frag =
    div(cls := s"sg-wrap d-${variant.numberOfFiles}x${variant.numberOfRanks}${orient
        .fold("")(o => s" orientation-${o.name}")} preload") {
      sgBoard {
        content
      }
    }

  def shogigroundBoard(variant: Variant, orient: Option[Color] = None) = sgWrap(variant, orient)(sgSquares)

}
