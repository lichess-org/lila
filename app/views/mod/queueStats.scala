package views.html.mod

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.mod.ModQueueStats._
import lila.mod.ModActivity.Period
import lila.report.Room

object queueStats {

  def apply(p: Result)(implicit ctx: Context) = {
    views.html.base.layout(
      title = "Queues stats",
      moreCss = cssTag("mod.activity"),
      moreJs = frag(
        jsModule("modActivity"),
        embedJsUnsafeLoadThen(s"""modActivity.queues(${safeJsonValue(p.json)})""")
      )
    ) {
      main(cls := "page-menu")(
        views.html.mod.menu("queues"),
        div(cls := "page-menu__content index box mod-queues")(
          h1(
            " Queues this ",
            periodSelector(p)
          ),
          div(cls := "chart-grid")
        )
      )
    }
  }

  private def periodSelector(p: Result) =
    views.html.base.bits
      .mselect(
        s"mod-activity__period-select box__top__actions",
        span(p.period.key),
        Period.all.map { per =>
          a(
            cls := (p.period == per).option("current"),
            href := routes.Mod.queues(per.key)
          )(per.toString)
        }
      )
}
