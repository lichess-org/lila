package views.html
package round

import play.twirl.api.Html

import lila.api.Context
import lila.app.templating.Environment._
import lila.game.{ Game, Pov, Player }
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object bits {

  def layout(
    title: String,
    side: Option[Frag],
    chat: Option[Frag] = None,
    underchat: Option[Frag] = None,
    moreJs: Html = emptyHtml,
    openGraph: Option[lila.app.ui.OpenGraph] = None,
    moreCss: Html = emptyHtml,
    chessground: Boolean = true,
    playing: Boolean = false,
    robots: Boolean = false
  )(body: Html)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      side = side.map(_.toHtml),
      chat = chat,
      underchat = underchat,
      openGraph = openGraph,
      moreJs = moreJs,
      moreCss = moreCss,
      chessground = chessground,
      playing = playing,
      robots = robots,
      asyncJs = true,
      zoomable = true
    )(body)

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
