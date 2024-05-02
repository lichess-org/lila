package views.round

import lila.app.UiEnv.{ *, given }
import lila.game.GameExt.playerBlurPercent

lazy val ui     = lila.round.ui.RoundUi(helpers, views.game.ui)
lazy val jsI18n = lila.round.ui.RoundI18n(helpers)

object bits:

  def crosstable(cross: Option[lila.game.Crosstable.WithMatchup], game: Game)(using ctx: Context) =
    cross.map: c =>
      views.game.ui.crosstable(ctx.userId.fold(c)(c.fromPov), game.id.some)

  def underchat(game: Game)(using ctx: Context) =
    frag(
      views.chat.spectatorsFrag,
      isGranted(_.ViewBlurs).option(
        div(cls := "round__mod")(
          game.players.all
            .filter(p => game.playerBlurPercent(p.color) > 30)
            .map { p =>
              div(
                playerLink(
                  p,
                  cssClass = s"is color-icon ${p.color.name}".some,
                  withOnline = false,
                  mod = true
                ),
                s" ${p.blurs.nb}/${game.playerMoves(p.color)} blurs ",
                strong(game.playerBlurPercent(p.color), "%")
              )
            }
            // game.players flatMap { p => p.holdAlert.map(p ->) } map {
            //   case (p, h) => div(
            //     playerLink(p, cssClass = s"is color-icon ${p.color.name}".some, mod = true, withOnline = false),
            //     "hold alert",
            //     br,
            //     s"(ply: ${h.ply}, mean: ${h.mean} ms, SD: ${h.sd})"
            //   )
            // }
        )
      )
    )

  private[round] def simulOtherGames(s: lila.simul.Simul)(using Context) =
    span(cls := "simul")(
      a(href := routes.Simul.show(s.id))("SIMUL"),
      span(cls := "win")(s.wins, " W"),
      " / ",
      span(cls := "draw")(s.draws, " D"),
      " / ",
      span(cls := "loss")(s.losses, " L"),
      " / ",
      s.ongoing,
      " ongoing"
    )

  private[round] def side(
      pov: Pov,
      data: play.api.libs.json.JsObject,
      tour: Option[lila.tournament.TourAndTeamVs],
      simul: Option[lila.simul.Simul],
      userTv: Option[User] = None,
      bookmarked: Boolean
  )(using Context) =
    import lila.common.Json.given
    views.game.side(
      pov,
      (data \ "game" \ "initialFen").asOpt[chess.format.Fen.Full],
      tour,
      simul = simul,
      userTv = userTv,
      bookmarked = bookmarked
    )
