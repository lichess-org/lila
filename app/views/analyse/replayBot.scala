package views.html.analyse

import play.twirl.api.Html

import bits.dataPanel
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.game.Pov

object replayBot {

  def apply(
    pov: Pov,
    initialFen: Option[chess.format.FEN],
    pgn: String,
    analysis: Option[lila.analyse.Analysis],
    simul: Option[lila.simul.Simul],
    cross: Option[lila.game.Crosstable.WithMatchup]
  )(implicit ctx: Context) = {

    import pov._

    views.html.analyse.bits.layout(
      title = s"${playerText(pov.player)} vs ${playerText(pov.opponent)} in $gameId : ${game.opening.fold(trans.analysis.txt())(_.opening.ecoName)}",
      side = views.html.game.side(pov, initialFen, none, simul = simul, bookmarked = false),
      chat = none,
      underchat = Some(views.html.game.bits.watchers),
      moreCss = cssTag("analyse.css"),
      openGraph = povOpenGraph(pov).some
    ) {
        frag(
          div(cls := "analyse cg-512")(
            views.html.board.bits.domPreload(pov.some),
            div(cls := "lichess_ground for_bot")(
              h1(titleGame(pov.game)),
              p(describePov(pov)),
              p(pov.game.opening.map(_.opening.ecoName))
            )
          ),
          analysis.map { a =>
            div(cls := "advice_summary")(
              table(
                a.summary map {
                  case (color, pairs) => frag(
                    thead(
                      tr(
                        td(span(cls := s"is color-icon $color"))
                      ),
                      th(playerLink(pov.game.player(color), withOnline = false))
                    ),
                    tbody(
                      pairs map {
                        case (judgment, nb) => tr(
                          td(strong(nb)),
                          th(bits.judgmentName(judgment))
                        )
                      },
                      tr(
                        td(strong(lila.analyse.Accuracy.mean(pov.withColor(color), a))),
                        th(trans.averageCentipawnLoss())
                      ),
                      tr(td(cls := "spacerlol", colspan := 2))
                    )
                  )
                }
              )
            )
          },
          div(cls := "underboard_content")(
            div(cls := "analysis_panels")(
              div(cls := "panel fen_pgn")(
                div(cls := "pgn")(pgn)
              ),
              cross.map { c =>
                div(cls := "panel crosstable")(
                  views.html.game.crosstable(pov.player.userId.fold(c)(c.fromPov), pov.gameId.some)
                )
              }
            ),
            div(cls := "analysis_menu")(
              cross.isDefined option a(dataPanel := "crosstable", cls := "crosstable")(trans.crosstable()),
              a(dataPanel := "fen_pgn", cls := "fen_pgn")(raw("FEN &amp; PGN"))
            )
          )
        )
      }
  }
}
