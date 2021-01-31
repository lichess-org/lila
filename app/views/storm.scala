package views.html

import controllers.routes
import play.api.i18n.Lang
import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.common.String.html.safeJsonValue
import lila.storm.{ StormDay, StormHigh }
import lila.user.User

object storm {

  def home(data: JsObject, pref: JsObject, high: Option[StormHigh])(implicit ctx: Context) =
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
      zoomable = true,
      chessground = false
    ) {
      main(
        div(cls := "storm storm-app storm--play")(
          div(cls := "storm__board main-board"),
          div(cls := "storm__side")
        ),
        high map { h =>
          frag(
            div(cls := "storm-play-scores")(
              span(trans.storm.highscores()),
              a(href := routes.Storm.dashboard())(trans.storm.viewBestRuns(), " »")
            ),
            div(cls := "storm-dashboard__high__periods")(
              renderHigh(h)
            )
          )
        },
        div(cls := "storm__about__link")(
          a(href := routes.Page.loneBookmark("storm"))("About Puzzle Storm")
        )
      )
    }

  private def renderHigh(high: StormHigh)(implicit lang: Lang) =
    frag(
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

  private val numberTag = tag("number")

  def dashboard(user: User, history: Paginator[StormDay], high: StormHigh)(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${user.username} Puzzle Storm",
      moreCss = frag(cssTag("storm.dashboard")),
      moreJs = infiniteScrollTag
    )(
      main(cls := "storm-dashboard page-small")(
        div(cls := "storm-dashboard__high box box-pad")(
          h1(
            !ctx.is(user) option frag(
              userLink(user),
              " • "
            ),
            "Puzzle Storm • ",
            trans.storm.highscores()
          ),
          div(cls := "storm-dashboard__high__periods highlight-alltime")(
            renderHigh(high)
          )
        ),
        a(cls := "storm-play-again button", href := routes.Storm.home())(trans.storm.playAgain()),
        div(cls := "storm-dashboard__history box")(
          table(cls := "slist slist-pad")(
            thead(
              tr(
                th(trans.storm.bestRunOfDay()),
                th(trans.storm.score()),
                th(trans.storm.moves()),
                th(trans.storm.accuracy()),
                th(trans.storm.combo()),
                th(trans.storm.time()),
                th(trans.storm.highestSolved()),
                th(trans.storm.runs())
              )
            ),
            tbody(cls := "infinite-scroll")(
              history.currentPageResults.map { day =>
                tr(
                  td(showDate(day._id.day.toDate)),
                  td(numberTag(cls := "score")(day.score)),
                  td(numberTag(day.moves)),
                  td(numberTag(f"${day.accuracyPercent}%1.1f"), "%"),
                  td(numberTag(day.combo)),
                  td(numberTag(day.time), "s"),
                  td(numberTag(day.highest)),
                  td(numberTag(day.runs))
                )
              },
              pagerNextTable(
                history,
                np =>
                  addQueryParameter(
                    if (ctx is user) routes.Storm.dashboard().url
                    else routes.Storm.dashboardOf(user.username).url,
                    "page",
                    np
                  )
              )
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
      t.playAgain,
      t.score,
      t.moves,
      t.accuracy,
      t.combo,
      t.time,
      t.timePerMove,
      t.highestSolved,
      t.puzzlesPlayed,
      t.newRun,
      t.endRun
    ).map(_.key)
  }
}
