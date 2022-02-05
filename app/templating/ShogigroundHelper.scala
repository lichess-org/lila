package lila.app
package templating

import shogi.{ Color, Pos, Situation }
import lila.api.Context

import lila.app.ui.ScalatagsTemplate._
import lila.common.Maths
import lila.game.Pov

trait ShogigroundHelper {

  private val cgWrap      = div(cls := "cg-wrap") // todo rename to sg
  private val cgContainer = tag("cg-container")
  private val cgBoard     = tag("cg-board")
  val cgWrapContent       = cgContainer(cgBoard)

  def shogiground(sit: Situation, orient: Color, lastMove: List[Pos] = Nil)(implicit ctx: Context): Frag =
    div(cls := s"cg-wrap orientation-${orient.name} variant-${sit.variant.key}") {
      cgBoard {
        raw {
          val offsetY = Maths.roundAt(100.0 / sit.variant.numberOfRanks, 3).toDouble
          val offsetX = Maths.roundAt(100.0 / sit.variant.numberOfFiles, 3).toDouble
          def top(p: Pos) =
            orient.fold(p.rank.index, sit.variant.numberOfRanks - p.rank.index - 1) * offsetY
          def left(p: Pos) =
            orient.fold(sit.variant.numberOfFiles - p.rank.index - 1, p.file.index) * offsetX
          val highlights = ctx.pref.highlight ?? lastMove.distinct.map { pos =>
            s"""<square class="last-move" style="top:${top(pos)}%;left:${left(pos)}%"></square>"""
          } mkString ""
          val pieces =
            if (ctx.pref.isBlindfold) ""
            else
              sit.board.pieces.map { case (pos, piece) =>
                val klass = s"${piece.color.name} ${piece.role.name}"
                s"""<piece class="$klass" style="top:${top(pos)}%;left:${left(pos)}%"></piece>"""
              } mkString ""
          s"$highlights$pieces"
        }
      }
    }

  def shogiground(pov: Pov)(implicit ctx: Context): Frag =
    shogiground(
      sit = pov.game.situation,
      orient = pov.color,
      lastMove = ~pov.game.history.lastMove.map(_.positions)
    )

  private def wrap(content: Frag): Frag =
    cgWrap {
      cgContainer {
        content
      }
    }

  lazy val shogigroundBoard = wrap(cgBoard)
}
