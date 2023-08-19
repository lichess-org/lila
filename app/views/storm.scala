package views.html

import controllers.routes
import play.api.i18n.Lang
import play.api.libs.json.*

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.LangPath
import lila.common.paginator.Paginator
import lila.common.String.html.safeJsonValue
import lila.storm.{ StormDay, StormHigh }
import lila.user.User

object storm:

  def home(data: JsObject, high: Option[StormHigh])(using PageContext) =
    views.html.base.layout(
      moreCss = frag(cssTag("storm")),
      moreJs = jsModuleInit("storm", data ++ Json.obj("i18n" -> i18nJsObject(i18nKeys))),
      title = "Puzzle Storm",
      zoomable = true,
      zenable = true,
      withHrefLangs = LangPath(routes.Storm.home).some
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
          a(href := routes.ContentPage.loneBookmark("storm"))(trans.aboutX("Puzzle Storm"))
        )
      )
    }

  private def renderHigh(high: StormHigh)(using Lang) =
    frag(
      List(
        (high.allTime, trans.storm.allTime),
        (high.month, trans.storm.thisMonth),
        (high.week, trans.storm.thisWeek),
        (high.day, trans.today)
      ).map { case (value, name) =>
        div(cls := "storm-dashboard__high__period")(
          strong(value),
          span(name())
        )
      }
    )

  private val numberTag = tag("number")

  def dashboard(user: User, history: Paginator[StormDay], high: StormHigh)(using ctx: PageContext) =
    views.html.base.layout(
      title = s"${user.username} Puzzle Storm",
      moreCss = frag(cssTag("storm.dashboard")),
      moreJs = infiniteScrollTag
    )(
      main(cls := "storm-dashboard page-small")(
        div(cls := "storm-dashboard__high box box-pad")(
          boxTop(
            h1(
              !ctx.is(user) option frag(
                userLink(user),
                " • "
              ),
              "Puzzle Storm • ",
              trans.storm.highscores()
            )
          ),
          div(cls := "storm-dashboard__high__periods highlight-alltime")(
            renderHigh(high)
          )
        ),
        a(cls := "storm-play-again button", href := routes.Storm.home)(trans.storm.playAgain()),
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
                  addQueryParam(
                    if ctx is user then routes.Storm.dashboard().url
                    else routes.Storm.dashboardOf(user.username).url,
                    "page",
                    np.toString
                  )
              )
            )
          )
        )
      )
    )

  private val i18nKeys =
    import lila.i18n.{ I18nKeys as trans }
    import lila.i18n.I18nKeys.{ storm as s }
    List(
      s.moveToStart,
      s.puzzlesSolved,
      s.newDailyHighscore,
      s.newWeeklyHighscore,
      s.newMonthlyHighscore,
      s.newAllTimeHighscore,
      s.previousHighscoreWasX,
      s.playAgain,
      s.score,
      s.moves,
      s.accuracy,
      s.combo,
      s.time,
      s.timePerMove,
      s.highestSolved,
      s.puzzlesPlayed,
      s.newRun,
      s.endRun,
      s.youPlayTheWhitePiecesInAllPuzzles,
      s.youPlayTheBlackPiecesInAllPuzzles,
      s.failedPuzzles,
      s.slowPuzzles,
      s.thisWeek,
      s.thisMonth,
      s.allTime,
      s.clickToReload,
      s.thisRunHasExpired,
      s.thisRunWasOpenedInAnotherTab,
      trans.flipBoard
    )
