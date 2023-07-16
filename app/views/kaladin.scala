package views.html

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes
import lila.irwin.KaladinUser

object kaladin:

  private def predClass(pred: KaladinUser.Pred) = pred.percent match
    case p if p < 30 => "green"
    case p if p < 60 => "yellow"
    case p if p < 80 => "orange"
    case _           => "red"

  def dashboard(dashboard: lila.irwin.KaladinUser.Dashboard)(using PageContext) =
    views.html.base.layout(
      title = "Kaladin dashboard",
      moreCss = cssTag("mod.misc")
    ) {
      main(cls := "page-menu")(
        mod.menu("kaladin"),
        div(cls := "kaladin page-menu__content box")(
          boxTop(
            h1(
              "Kaladin status: ",
              if dashboard.seenRecently then span(cls := "up")("Operational")
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
                  td(userIdLink(entry._id.some, params = "?mod")),
                  td(cls := "little")(momentFromNow(entry.queuedAt)),
                  td(cls := "little")(entry.startedAt map { momentFromNow(_) }),
                  td(cls := "little completed")(entry.response.map(_.at) map { momentFromNow(_) }),
                  td {
                    entry.queuedBy match
                      case KaladinUser.Requester.Mod(id) => userIdLink(id.some)
                      case requester                     => em(requester.name)
                  },
                  entry.response.fold(td) { res =>
                    res.pred
                      .map { pred =>
                        td(cls := s"little activation ${predClass(pred)}")(
                          strong(pred.percent)
                        )
                      }
                      .orElse {
                        res.err.map { err =>
                          td(cls := "error")(err)
                        }
                      }
                      .|(td)
                  }
                )
              }
            )
          )
        )
      )
    }

  def report(response: lila.irwin.KaladinUser.Response): Frag =
    div(cls := "mz-section mz-section--kaladin", dataRel := "kaladin")(
      header(
        span(cls := "title")(
          a(href := routes.Irwin.kaladin)("Kaladin")
        ),
        div(cls := "infos")(
          p("Updated ", momentFromNowServer(response.at))
        ),
        response.pred.map { pred =>
          div(cls := "assess text")(
            strong(cls := predClass(pred))(pred.percent),
            " Overall assessment"
          )
        }
      ),
      response.pred.map { pred =>
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
      }
    )
