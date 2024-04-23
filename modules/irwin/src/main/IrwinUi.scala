package lila.irwin

import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.i18n.Translate
import lila.ui.Context
import lila.core.game.Pov

final class IrwinUi(
    i18nHelper: lila.ui.I18nHelper,
    dateHelper: lila.ui.DateHelper,
    userHelper: lila.ui.UserHelper
)(
    povLink: Pov => Context ?=> Frag,
    playerBlurPercent: Pov => Int,
    routeRoundWatcher: (String, String) => Call
):
  import i18nHelper.{ *, given }
  import dateHelper.*

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
                  a(href := routeRoundWatcher(pov.gameId, pov.color.name))(
                    gameReport.moves.map: move =>
                      span(
                        cls      := percentClass(move.activation),
                        st.title := move.toString,
                        style    := s"height:${move.activation}%"
                      )
                  )
                ),
                td(
                  povLink(pov)
                ),
                td(
                  strong(cls := percentClass(gameReport.activation))(gameReport.activation, "%"),
                  " ",
                  em("assessment")
                ),
                td {
                  val blurs = playerBlurPercent(pov)
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

  def dashboard(dashboard: IrwinReport.Dashboard)(using Context) =
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
              td(userHelper.userIdLink(rep.suspectId.value.some)),
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
            p("Updated ", dateHelper.momentFromNowServer(response.at))
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
                td(userHelper.userIdLink(entry.id.some, params = "?mod")),
                td(cls := "little")(momentFromNow(entry.queuedAt)),
                td(cls := "little")(entry.startedAt.map { momentFromNow(_) }),
                td(cls := "little completed")(entry.response.map(_.at).map { momentFromNow(_) }),
                td(
                  entry.queuedBy match
                    case KaladinUser.Requester.Mod(id) => userHelper.userIdLink(id.some)
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
