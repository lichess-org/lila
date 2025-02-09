package lila.storm
package ui

import play.api.libs.json.*
import scalalib.paginator.Paginator

import lila.core.id.CmsPageKey
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class StormUi(helpers: Helpers):
  import helpers.{ *, given }

  def home(data: JsObject, high: Option[StormHigh])(using Context) =
    Page("Puzzle Storm")
      .css("storm")
      .i18n(_.storm)
      .js(PageModule("storm", data))
      .flag(_.zoom)
      .flag(_.zen)
      .hrefLangs(lila.ui.LangPath(routes.Storm.home)):
        main(
          div(cls := "storm storm-app storm--play")(
            div(cls := "storm__board main-board"),
            div(cls := "storm__side")
          ),
          high.map { h =>
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
            a(href := routes.Cms.lonePage(CmsPageKey("storm")))(trans.site.aboutX("Puzzle Storm"))
          )
        )

  private def renderHigh(high: StormHigh)(using Translate) =
    frag(
      List(
        (high.allTime, trans.storm.allTime),
        (high.month, trans.storm.thisMonth),
        (high.week, trans.storm.thisWeek),
        (high.day, trans.site.today)
      ).map: (value, name) =>
        div(cls := "storm-dashboard__high__period")(
          strong(value),
          span(name())
        )
    )

  private val numberTag = tag("number")

  def dashboard(user: User, history: Paginator[StormDay], high: StormHigh)(using ctx: Context) =
    Page(s"${user.username} Puzzle Storm")
      .css("storm.dashboard")
      .js(infiniteScrollEsmInit):
        main(cls := "storm-dashboard page-small")(
          div(cls := "storm-dashboard__high box box-pad")(
            boxTop(
              h1(
                ctx
                  .isnt(user)
                  .option(
                    frag(
                      userLink(user),
                      " • "
                    )
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
                      if ctx.is(user) then routes.Storm.dashboard().url
                      else routes.Storm.dashboardOf(user.username).url,
                      "page",
                      np.toString
                    )
                )
              )
            )
          )
        )
