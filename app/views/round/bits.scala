package views.html
package round

import scalatags.Text.all._

import lila.api.Context
import lila.app.templating.Environment._
import lila.game.{ Game, Pov, Player }
import lila.i18n.{ I18nKeys => trans }

import controllers.routes

object bits {

  def underboard(game: Game, cross: Option[lila.game.Crosstable.WithMatchup])(implicit ctx: Context) =
    div(cls := "underboard")(
      div(cls := "center")(
        cross map { c =>
          div(cls := "crosstable")(
            views.html.game.crosstable(ctx.userId.fold(c)(c.fromPov), game.id.some)
          )
        }
      )
    )

  def underchat(game: Game)(implicit ctx: Context) = frag(
    views.html.game.bits.watchers,
    isGranted(_.ViewBlurs) option frag(
      game.players.filter(p => game.playerBlurPercent(p.color) > 30) map { p =>
        frag(
          br,
          span(cls := "mod blurs")(
            playerLink(p, cssClass = s"is color-icon ${p.color.name}".some, withOnline = false, mod = true),
            s"${p.blurs.nb}/${game.playerMoves(p.color)} blurs",
            strong(game.playerBlurPercent(p.color), "%")
          )
        )
      },
      game.players flatMap { p => p.holdAlert.map(p ->) } map {
        case (p, h) => frag(
          br,
          span(cls := "mod hold")(
            playerLink(p, cssClass = s"is color-icon ${p.color.name}".some, mod = true, withOnline = false),
            "hold alert",
            br,
            s"(ply: ${h.ply}, mean: ${h.mean} ms, SD: ${h.sd})"
          ),
          br
        )
      }
    )
  )
}
