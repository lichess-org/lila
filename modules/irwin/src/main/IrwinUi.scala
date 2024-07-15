package lila.irwin

import lila.core.game.Pov
import lila.game.GameExt.playerBlurPercent
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class IrwinUi(helpers: Helpers)(menu: String => Context ?=> Frag):
  import helpers.{ *, given }

  private def povLink(pov: Pov)(using Context) =
    a(href := routes.Round.watcher(pov.gameId, pov.color))(
      playerLink(
        pov.opponent,
        withRating = true,
        withDiff = true,
        withOnline = false,
        link = false
      ),
      br,
      pov.game.isTournament.so(frag(iconTag(Icon.Trophy), " ")),
      iconTag(pov.game.perfKey.perfIcon),
      shortClockName(pov.game.clock.map(_.config)),
      " ",
      momentFromNowServer(pov.game.createdAt)
    )

  def percentClass(p: Int) =
    if p < 30 then "green"
    else if p < 60 then "yellow"
    else if p < 80 then "orange"
    else "red"

  def report(report: IrwinReport.WithPovs)(using Context): Frag =
    div(cls := "mz-section mz-section--irwin", dataRel := "irwin")(
      header(
        a(cls := "title", href := "/irwin")("Irwin AI"),
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
            case IrwinReport.GameReport.WithPov(gameReport, pov) =>
              tr(cls := "text")(
                td(cls := "moves")(
                  a(href := routes.Round.watcher(pov.gameId, pov.color))(
                    gameReport.moves.map: move =>
                      span(
                        cls      := percentClass(move.activation),
                        st.title := move.toString,
                        style    := s"height:${move.activation}%"
                      )
                  )
                ),
                td(povLink(pov)),
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

  private def DashPage(title: String, active: String)(using Context) =
    Page(title)
      .css("mod.misc")
      .wrap: body =>
        main(cls := "page-menu")(
          menu(active),
          body
        )

  def dashboard(dashboard: IrwinReport.Dashboard)(using Context) =
    DashPage("Irwin dashboard", "irwin"):
      div(cls := "irwin page-menu__content box")(
        boxTop(
          h1(
            "Irwin status: ",
            if dashboard.seenRecently then span(cls := "up")("Operational")
            else
              span(cls := "down")(
                dashboard.lastSeenAt.fold(frag("Unknown")): seenAt =>
                  frag("Last seen ", momentFromNow(seenAt))
              )
          ),
          div(cls := "box__top__actions")(
            a(
              href := "https://monitor.lichess.ovh/d/a5qOnu9Wz/mod-yield",
              cls  := "button button-empty"
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
            dashboard.recent.map: rep =>
              tr(cls := "report")(
                td(userIdLink(rep.suspectId.value.some)),
                td(cls := "little completed")(momentFromNow(rep.date)),
                td(rep.owner),
                td(cls := s"little activation ${percentClass(rep.activation)}")(
                  strong(rep.activation, "%"),
                  " over ",
                  rep.games.size,
                  " games"
                )
              )
          )
        )
      )

  object kaladin:

    def report(response: KaladinUser.Response)(using Translate): Frag =
      div(cls := "mz-section mz-section--kaladin", dataRel := "kaladin")(
        header(
          span(cls := "title")(
            a(href := "/kaladin")("Kaladin")
          ),
          div(cls := "infos")(
            p("Updated ", momentFromNowServer(response.at))
          ),
          response.pred.map: pred =>
            div(cls := "assess text")(
              strong(cls := percentClass(pred.percent))(pred.percent),
              " Overall assessment"
            )
        ),
        response.pred.map: pred =>
          frag(
            div("Top insights (by order of relevance)"),
            table(cls := "slist")(
              tbody(
                tr(cls := "text")(pred.insights.map { insight =>
                  td(a(href := insight)(insight.split("/").drop(5).mkString("/")))
                })
              )
            )
          )
      )

    def dashboard(dashboard: KaladinUser.Dashboard)(using Context) =
      DashPage("Kaladin dashboard", "kaladin"):
        div(cls := "kaladin page-menu__content box")(
          boxTop(
            h1(
              "Kaladin status: ",
              if dashboard.seenRecently then span(cls := "up")("Operational")
              else
                span(cls := "down")(
                  dashboard.lastSeenAt
                    .map: seenAt =>
                      frag("Last seen ", momentFromNow(seenAt))
                    .getOrElse {
                      frag("Unknown")
                    }
                )
            ),
            div(cls := "box__top__actions")(
              a(
                href := "https://monitor.lichess.ovh/d/a5qOnu9Wz/mod-yield",
                cls  := "button button-empty"
              )("Monitoring")
            )
          ),
          table(cls := "slist slist-pad")(
            thead(
              tr(
                th("Recent request"),
                th("Queued"),
                th("Started"),
                th("Completed"),
                th("Requester"),
                th("Score")
              )
            ),
            tbody(
              dashboard.recent.map { entry =>
                tr(cls := "report")(
                  td(userIdLink(entry.id.some, params = "?mod")),
                  td(cls := "little")(momentFromNow(entry.queuedAt)),
                  td(cls := "little")(entry.startedAt.map { momentFromNow(_) }),
                  td(cls := "little completed")(entry.response.map(_.at).map { momentFromNow(_) }),
                  td(
                    entry.queuedBy match
                      case KaladinUser.Requester.Mod(id) => userIdLink(id.some)
                      case requester                     => em(requester.name)
                  ),
                  entry.response.fold(td): res =>
                    res.pred
                      .map: pred =>
                        td(cls := s"little activation ${percentClass(pred.percent)}")(
                          strong(pred.percent)
                        )
                      .orElse {
                        res.err.map: err =>
                          td(cls := "error")(err)
                      }
                      .getOrElse(td)
                )
              }
            )
          )
        )
