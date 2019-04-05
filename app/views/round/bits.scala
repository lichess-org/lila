package views.html
package round

import play.twirl.api.Html

import chess.variant.{ Variant, Crazyhouse }
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.game.{ Game, Pov, Player }

import controllers.routes

object bits {

  def layout(
    variant: Variant,
    title: String,
    moreJs: Html = emptyHtml,
    openGraph: Option[lila.app.ui.OpenGraph] = None,
    moreCss: Html = emptyFrag,
    chessground: Boolean = true,
    playing: Boolean = false,
    robots: Boolean = false
  )(body: Html)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      openGraph = openGraph,
      moreJs = moreJs,
      moreCss = frag(
        responsiveCssTag {
          if (variant == Crazyhouse) "round.zh"
          else "round"
        },
        moreCss
      ),
      responsive = true,
      chessground = chessground,
      playing = playing,
      robots = robots,
      asyncJs = true,
      zoomable = true
    )(body)

  def crosstable(cross: Option[lila.game.Crosstable.WithMatchup], game: Game)(implicit ctx: Context) =
    cross map { c =>
      views.html.game.crosstable(ctx.userId.fold(c)(c.fromPov), game.id.some)
    }

  def underchat(game: Game)(implicit ctx: Context) = frag(
    views.html.game.bits.watchers,
    isGranted(_.ViewBlurs) option div(cls := "round__mod")(
      game.players.filter(p => game.playerBlurPercent(p.color) > 30) map { p =>
        div(
          playerLink(p, cssClass = s"is color-icon ${p.color.name}".some, withOnline = false, mod = true),
          s"${p.blurs.nb}/${game.playerMoves(p.color)} blurs",
          strong(game.playerBlurPercent(p.color), "%")
        )
      },
      game.players flatMap { p => p.holdAlert.map(p ->) } map {
        case (p, h) => div(
          playerLink(p, cssClass = s"is color-icon ${p.color.name}".some, mod = true, withOnline = false),
          "hold alert",
          br,
          s"(ply: ${h.ply}, mean: ${h.mean} ms, SD: ${h.sd})"
        )
      }
    )
  )

  def others(playing: List[Pov], simul: Option[lila.simul.Simul])(implicit ctx: Context) = frag(
    h3(
      simul.map { s =>
        span(cls := "simul")(
          "SIMUL",
          span(cls := "win")(s.wins, " W"), " / ",
          span(cls := "draw")(s.draws, " D"), " / ",
          span(cls := "loss")(s.losses, " L"), " / ",
          s.ongoing, " ongoing"
        )
      } getOrElse trans.currentGames.frag(),
      "round-toggle-autoswitch" |> { id =>
        span(cls := "move-on switcher", st.title := trans.automaticallyProceedToNextGameAfterMoving.txt())(
          label(`for` := id)(trans.autoSwitch.frag()),
          span(cls := "switch")(
            input(st.id := id, cls := "cmn-toggle", tpe := "checkbox"),
            label(`for` := id)
          )
        )
      }
    ),
    div(cls := "now-playing")(
      playing.partition(_.isMyTurn) |> {
        case (myTurn, otherTurn) =>
          (myTurn ++ otherTurn.take(6 - myTurn.size)) take 9 map { pov =>
            a(href := routes.Round.player(pov.fullId), cls := pov.isMyTurn.option("my_turn"))(
              gameFen(pov, withLink = false, withTitle = false, withLive = false),
              span(cls := "meta")(
                playerText(pov.opponent, withRating = false),
                span(cls := "indicator")(
                  if (pov.isMyTurn) pov.remainingSeconds.fold(trans.yourTurn())(secondsFromNow(_, true))
                  else nbsp
                )
              )
            )
          }
      }
    )
  )
}
