package views.html.mod

import controllers.routes
import play.api.libs.json.Json

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.safeJsonValue
import lila.mod.ModActivity.*

object activity:

  def apply(p: Result)(using PageContext) =
    views.html.base.layout(
      title = "Moderation activity",
      moreCss = cssTag("mod.activity"),
      moreJs =
        jsModuleInit("mod.activity", Json.obj("op" -> "activity", "data" -> lila.mod.ModActivity.json(p)))
    ) {
      main(cls := "page-menu")(
        views.html.mod.menu("activity"),
        div(cls := "page-menu__content index box mod-activity")(
          boxTop(
            h1(
              whoSelector(p),
              " activity this ",
              periodSelector(p)
            )
          ),
          div(cls := "chart chart-reports"),
          div(cls := "chart chart-actions")
        )
      )
    }

  private def whoSelector(p: Result) =
    views.html.base.bits
      .mselect(
        s"mod-activity__who-select box__top__actions",
        span(if p.who == Who.Team then "Team" else "My"),
        List(
          a(
            cls  := (p.who == Who.Team).option("current"),
            href := routes.Mod.activityOf("team", p.period.key)
          )("Team"),
          a(
            cls  := (p.who != Who.Team).option("current"),
            href := routes.Mod.activityOf("me", p.period.key)
          )("My")
        )
      )

  private def periodSelector(p: Result) =
    views.html.base.bits
      .mselect(
        s"mod-activity__period-select box__top__actions",
        span(p.period.key),
        Period.values.toList.map { per =>
          a(
            cls  := (p.period == per).option("current"),
            href := routes.Mod.activityOf(p.who.key, per.key)
          )(per.toString)
        }
      )
