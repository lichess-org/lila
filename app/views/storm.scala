package views.html

import controllers.routes
import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.common.String.html.safeJsonValue
import lila.storm.{ StormDay, StormHigh }
import lila.user.User

object storm {

  def home(data: JsObject, pref: JsObject)(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("storm")),
      moreJs = frag(
        jsModule("storm"),
        embedJsUnsafeLoadThen(
          s"""LichessStorm.start(${safeJsonValue(
            Json.obj(
              "data" -> data,
              "pref" -> pref,
              "i18n" -> i18nJsObject(i18nKeys)
            )
          )})"""
        )
      ),
      title = "Puzzle Storm",
      zoomable = true
    ) {
      main(
        div(cls := "storm storm-app"),
        div(cls := "storm-links box box-pad")(
          a(href := routes.Storm.dashboard())("My Puzzle Storm dashboard")
        )
      )
    }

  def dashboard(user: User, history: Paginator[StormDay], high: StormHigh)(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("storm.dashboard")),
      title = s"${user.username} Puzzle Storm"
    )(
      main(cls := "storm-dashboard page-small")(
        div(cls := "storm-dashboard__high box box-pad")(
          h1(
            userLink(user),
            " • Puzzle Storm highscores"
          ),
          div(cls := "storm-dashboard__high__periods")(
            List(
              (high.allTime, "All-time"),
              (high.month, "This month"),
              (high.week, "This week"),
              (high.day, "Today")
            ).map { case (value, name) =>
              div(cls := "storm-dashboard__high__period")(
                strong(value),
                span(name)
              )
            }
          )
        ),
        a(cls := "storm-play-again button", href := routes.Storm.home())(trans.storm.playAgain()),
        div(cls := "storm-dashboard__history box")(
          table(cls := "slist slist-pad")(
            thead(
              tr(
                th("Session"),
                th("Highscore"),
                th("Most moves"),
                th("Max combo"),
                th("Max time"),
                th("Highest solved"),
                th("Runs")
              )
            ),
            tbody(
              history.currentPageResults.map { day =>
                tr(
                  td(momentFromNowServer(day._id.day.toDate)),
                  td(day.score),
                  td(day.moves),
                  td(day.combo),
                  td(day.time),
                  td(day.highest),
                  td(day.runs)
                )
              },
              pagerNextTable(history, np => addQueryParameter(routes.Storm.dashboard().url, "page", np))
            )
          )
        )
      )
    )

  private val i18nKeys = {
    import lila.i18n.I18nKeys.{ storm => t }
    List(
      t.moveToStart,
      t.puzzlesSolved,
      t.newDailyHighscore,
      t.newWeeklyHighscore,
      t.newMonthlyHighscore,
      t.newAllTimeHighscore,
      t.previousHighscoreWasX,
      t.playAgain
    ).map(_.key)
  }
}
