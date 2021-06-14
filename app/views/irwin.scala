package views.html

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object irwin {

  private def percentClass(percent: Int) =
    percent match {
      case p if p < 30 => "green"
      case p if p < 60 => "yellow"
      case p if p < 80 => "orange"
      case _           => "red"
    }

  def dashboard(dashboard: lila.irwin.IrwinDashboard)(implicit ctx: Context) =
    views.html.base.layout(
      title = "Irwin dashboard",
      moreCss = cssTag("mod.misc")
    ) {
      main(cls := "page-menu")(
        mod.menu("irwin"),
        div(cls := "irwin page-menu__content box")(
          div(cls := "box__top")(
            h1(
              "Irwin status: ",
              if (dashboard.seenRecently) span(cls := "up")("Operational")
              else
                span(cls := "down")(
                  dashboard.lastSeenAt.map { seenAt =>
                    frag("Last seen ", momentFromNow(seenAt))
                  } getOrElse {
                    frag("Unknown")
                  }
                )
            ),
            div(cls := "box__top__actions")(
              a(
                href := "https://monitor.lichess.ovh/d/a5qOnu9Wz/mod-yield",
                cls := "button button-empty"
              )("Monitoring")
            )
          ),
          table(cls := "slist slist-pad")(
            thead(
              tr(
                th("Recent report"),
                th("Completed"),
                th("Owner"),
                th("Activation")
              )
            ),
            tbody(
              dashboard.recent.map { rep =>
                tr(cls := "report")(
                  td(userIdLink(rep.suspectId.value.some, params = "?mod")),
                  td(cls := "little completed")(momentFromNow(rep.date)),
                  td(rep.owner),
                  td(cls := s"little activation ${percentClass(rep.activation)}")(
                    strong(rep.activation, "%"),
                    " over ",
                    rep.games.size,
                    " games"
                  )
                )
              }
            )
          )
        )
      )
    }

  def report(report: lila.irwin.IrwinReport.WithPovs)(implicit ctx: Context): Frag =
    div(cls := "mz-section mz-section--irwin", dataRel := "irwin")(
      header(
        a(cls := "title", href := routes.Irwin.dashboard)(
          img(src := assetUrl("images/icons/brain.blue.svg")),
          " Irwin AI",
          br,
          "Hunter"
        ),
        div(cls := "infos")(
          p("Updated ", momentFromNowServer(report.report.date))
        ),
        div(cls := "assess text")(
          strong(cls := percentClass(report.report.activation))(report.report.activation, "%"),
          " Overall assessment"
        )
      ),
      table(cls := "slist")(
        tbody(
          report.withPovs.sortBy(-_.report.activation).map {
            case lila.irwin.IrwinReport.GameReport.WithPov(gameReport, pov) =>
              tr(cls := "text")(
                td(cls := "moves")(
                  a(href := routes.Round.watcher(pov.gameId, pov.color.name))(
                    gameReport.moves.map { move =>
                      span(
                        cls := percentClass(move.activation),
                        st.title := move.toString,
                        style := s"height:${move.activation}%"
                      )
                    }
                  )
                ),
                td(
                  a(href := routes.Round.watcher(pov.gameId, pov.color.name))(
                    playerLink(
                      pov.opponent,
                      withRating = true,
                      withDiff = true,
                      withOnline = false,
                      link = false
                    ),
                    br,
                    pov.game.isTournament ?? frag(iconTag(""), " "),
                    pov.game.perfType.map { pt =>
                      iconTag(pt.iconChar)
                    },
                    shortClockName(pov.game.clock.map(_.config)),
                    " ",
                    momentFromNowServer(pov.game.createdAt)
                  )
                ),
                td(
                  strong(cls := percentClass(gameReport.activation))(gameReport.activation, "%"),
                  " ",
                  em("assessment")
                ),
                td {
                  val blurs = pov.game.playerBlurPercent(pov.color)
                  frag(strong(cls := percentClass(blurs))(blurs, "%"), " ", em("blurs"))
                }
                // td(
                //   pov.player.holdAlert.exists(_.suspicious) option strong(cls := percentClass(50))("Bot?")
                // )
              )
          }
        )
      )
    )
}
