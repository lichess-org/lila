package lidraughts.app
package templating

import draughts.{ Color, Board, Pos, PosMotion }
import lidraughts.api.Context
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.game.Pov

trait DraughtsgroundHelper {

  private val cgWrap = div(cls := "cg-wrap")
  private val cgHelper = tag("cg-helper")
  private val cgContainer = tag("cg-container")
  private val cgBoard = tag("cg-board")
  val cgWrapContent = cgHelper(cgContainer(cgBoard))

  def draughtsground(board: Board, orient: Color, lastMove: List[Pos] = Nil)(implicit ctx: Context): Frag = wrap {
    cgBoard {
      raw {
        def addX(p: PosMotion) = if (p.y % 2 != 0) -0.5 else -1.0
        def top(p: PosMotion) = orient.fold(p.y - 1, 10 - p.y) * 10.0
        def left(p: PosMotion) = orient.fold(addX(p) + p.x, 4.5 - (addX(p) + p.x)) * 20.0
        val highlights = ctx.pref.highlight ?? lastMove.distinct.map { pos =>
          val pm = board.posAt(pos)
          s"""<square class="last-move" style="top:${top(pm)}%;left:${left(pm)}%"></square>"""
        } mkString ""
        val pieces =
          if (ctx.pref.isBlindfold) ""
          else board.pieces.map {
            case (pos, piece) =>
              val klass = s"${piece.color.name} ${piece.role.name}"
              val pm = board.posAt(pos)
              s"""<piece class="$klass" style="top:${top(pm)}%;left:${left(pm)}%"></piece>"""
          } mkString ""
        s"$highlights$pieces"
      }
    }
  }

  def draughtsground(pov: Pov)(implicit ctx: Context): Frag = draughtsground(
    board = pov.game.board,
    orient = pov.color,
    lastMove = pov.game.history.lastMove.map(_.origDest) ?? {
      case (orig, dest) => List(orig, dest)
    }
  )

  private def wrap(content: Frag): Frag = cgWrap {
    cgHelper {
      cgContainer {
        content
      }
    }
  }

  lazy val draughtsgroundBoard = wrap(cgBoard)
}
