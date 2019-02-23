package views.html.analyse

import play.twirl.api.Html

import bits.dataPanel
import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.game.Pov

object replayBot {

  def apply(
    pov: Pov,
    initialFen: Option[draughts.format.FEN],
    pdn: String,
    analysis: Option[lidraughts.analyse.Analysis],
    simul: Option[lidraughts.simul.Simul],
    cross: Option[lidraughts.game.Crosstable.WithMatchup]
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
            div(cls := "lidraughts_ground for_bot")(
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
                        td(strong(lidraughts.analyse.Accuracy.mean(pov.withColor(color), a))),
                        th(trans.averageCentipieceLoss())
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
              div(cls := "panel fen_pdn")(
                div(cls := "pdn")(pdn)
              ),
              cross.map { c =>
                views.html.game.crosstable(pov.player.userId.fold(c)(c.fromPov), pov.gameId.some)
              }
            ),
            div(cls := "analysis_menu")(
              cross.isDefined option a(dataPanel := "crosstable", cls := "crosstable")(trans.crosstable()),
              a(dataPanel := "fen_pdn", cls := "fen_pdn")(raw("FEN &amp; PDN"))
            )
          )
        )
      }
  }
}
