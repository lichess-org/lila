package lila.app
package templating

import shogi.{ Color, Hand, Pos, Situation }
import shogi.variant.Variant
import lila.api.Context

import lila.app.ui.ScalatagsTemplate._
import lila.game.Pov

trait ShogigroundHelper {

  private val sgBoard   = tag("sg-board")
  private val sgSquares = tag("sg-squares")
  private val sgPieces  = tag("sg-pieces")

  private val sgHandTop    = tag("sg-hand-wrap")(cls := "hand-top")
  private val sgHandBottom = tag("sg-hand-wrap")(cls := "hand-bottom")
  private val sgHand       = tag("sg-hand")

  def shogiground(sit: Situation, orient: Color, @scala.annotation.unused lastUsi: List[Pos] = Nil)(implicit
      ctx: Context
  ): Frag =
    sgWrap(sit.variant, orient) {
      frag(
        sit.variant.supportsDrops option sgHandTop(
          sgHand(shogigroundHandPieces(sit.variant, sit.hands(!orient), !orient))
        ),
        sgBoard(
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
        ),
        sit.variant.supportsDrops option sgHandBottom(
          sgHand(shogigroundHandPieces(sit.variant, sit.hands(orient), orient))
        )
      )
    }

  def shogiground(pov: Pov)(implicit ctx: Context): Frag =
    shogiground(
      sit = pov.game.situation,
      orient = pov.color,
      lastUsi = ~pov.game.history.lastUsi.map(_.positions)
    )

  def shogigroundEmpty(variant: Variant, orient: Color) =
    sgWrap(variant, orient)(
      frag(
        variant.supportsDrops option sgHandTop(sgHand),
        sgBoard(sgSquares),
        variant.supportsDrops option sgHandBottom(sgHand)
      )
    )

  private def shogigroundHandPieces(variant: Variant, hand: Hand, color: Color): Frag =
    raw {
      variant.handRoles.map { role =>
        s"""<sg-hp-wrap data-nb="${hand(
            role
          )}"><piece class="${color.name} ${role.name}"></piece></sg-hp-wrap>"""
      } mkString ""
    }

  private def sgWrap(variant: Variant, orient: Color)(content: Frag): Frag =
    div(
      cls := s"sg-wrap d-${variant.numberOfFiles}x${variant.numberOfRanks} orientation-${orient.name} preload"
    )(
      content
    )

}
