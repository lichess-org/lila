package views.html.mod

import controllers.routes
import play.api.libs.json.Json

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.safeJsonValue
import lila.mod.ModQueueStats.*
import lila.mod.ModActivity.Period

object queueStats:

  def apply(p: Result)(using PageContext) =
    views.html.base.layout(
      title = "Queues stats",
      moreCss = cssTag("mod.activity"),
      moreJs = jsModuleInit("mod.activity", Json.obj("op" -> "queues", "data" -> p.json))
    ):
      main(cls := "page-menu")(
        views.html.mod.menu("queues"),
        div(cls := "page-menu__content index box mod-queues")(
          boxTop(
            h1(
              " Queues this ",
              periodSelector(p)
            )
          ),
          div(cls := "chart-grid")
        )
      )

  private def periodSelector(p: Result) =
    views.html.base.bits.mselect(
      s"mod-activity__period-select box__top__actions",
      span(p.period.key),
      Period.values.toList.map: per =>
        a(
          cls  := (p.period == per).option("current"),
          href := routes.Mod.queues(per.key)
        )(per.toString)
    )
