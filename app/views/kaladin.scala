package views.html

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes
import lila.irwin.KaladinUser

object kaladin {

  private def predClass(pred: Float) = pred match {
    case p if p < 0.3 => "green"
    case p if p < 0.6 => "yellow"
    case p if p < 0.8 => "orange"
    case _            => "red"
  }

  def dashboard(dashboard: lila.irwin.KaladinUser.Dashboard)(implicit ctx: Context) =
    views.html.base.layout(
      title = "Kaladin dashboard",
      moreCss = cssTag("mod.misc")
    ) {
      main(cls := "page-menu")(
        mod.menu("kaladin"),
        div(cls := "kaladin page-menu__content box")(
          div(cls := "box__top")(
            h1(
              "Kaladin status: ",
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
                th("Recent request"),
                th("Queued"),
                th("Started"),
                th("Completed"),
                th("Owner"),
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
                    entry.queuedBy match {
                      case KaladinUser.Requester.Mod(id) => userIdLink(id.some)
                      case requester                     => em(requester.name)
                    }
                  },
                  entry.response.fold(td) { res =>
                    td(cls := s"little activation ${predClass(res.pred.activation)}")(
                      strong(res.pred.activation)
                    )
                  }
                )
              }
            )
          )
        )
      )
    }
}
