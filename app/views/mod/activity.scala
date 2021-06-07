package views.html.mod

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.mod.ModActivity._
import lila.report.Room

object activity {

  def apply(p: Result)(implicit ctx: Context) = {
    views.html.base.layout(
      title = "Moderation activity",
      moreCss = cssTag("mod.activity"),
      moreJs = frag(
        jsModule("modActivity"),
        embedJsUnsafeLoadThen(s"""modActivity(${safeJsonValue(lila.mod.ModActivity.json(p))})""")
      )
    ) {
      main(cls := "page-menu")(
        views.html.mod.menu("activity"),
        div(cls := "page-menu__content index box mod-activity")(
          h1(
            whoSelector(p),
            " activity this ",
            periodSelector(p)
          ),
          div(cls := "chart chart-reports"),
          div(cls := "chart chart-actions")
          // renderTable(p)
        )
      )
    }
  }

  private def whoSelector(p: Result) =
    views.html.base.bits
      .mselect(
        s"mod-activity__who-select box__top__actions",
        span(if (p.who == Who.Team) "Team" else "My"),
        List(
          a(
            cls := (p.who == Who.Team).option("current"),
            href := routes.Mod.activityOf("team", p.period.key)
          )("Team"),
          a(
            cls := (p.who != Who.Team).option("current"),
            href := routes.Mod.activityOf("me", p.period.key)
          )("My")
        )
      )

  private def periodSelector(p: Result) =
    views.html.base.bits
      .mselect(
        s"mod-activity__period-select box__top__actions",
        span(p.period.key),
        List(Period.Week, Period.Month, Period.Year).map { per =>
          a(
            cls := (p.period == per).option("current"),
            href := routes.Mod.activityOf(p.who.key, per.key)
          )(per.toString)
        }
      )

  private def renderTable(p: Result)(implicit ctx: Context) =
    table(cls := "slist slist-pad history")(
      thead(
        tr(
          th("Date"),
          Room.all.map { r =>
            th("Report", br, r.name)
          },
          Action.all.map { a =>
            th("Action", br, a.toString)
          }
        )
      ),
      tbody(
        p.data.view
          .take(30)
          .map { case (date, row) =>
            tr(
              th(showDate(date)),
              Room.all.map { r =>
                td(~row.reports.get(r))
              },
              Action.all.map { a =>
                td(~row.actions.get(a))
              }
            )
          }
          .toList
      )
    )
}
