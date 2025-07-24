package lila.mod
package ui

import lila.mod.Gamify.Period
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class GamifyUi(helpers: Helpers, modUi: ModUi):
  import helpers.{ *, given }

  def index(leaderboards: Gamify.Leaderboards, history: List[Gamify.HistoryMonth])(using ctx: Context) =
    val title                 = "Moderator hall of fame"
    def yearHeader(year: Int) =
      tr(cls := "year")(
        th(year),
        th("Champions of the past"),
        th("Score"),
        th("Actions taken"),
        th("Report points")
      )

    Page(title).css("mod.gamify"):
      main(cls := "page-menu")(
        bits.modMenu("gamify"),
        div(id := "mod-gamify", cls := "page-menu__content index box")(
          h1(cls := "box__top")(title),
          div(cls := "champs")(
            champion(leaderboards.daily.headOption, "reward1", Period.Day),
            champion(leaderboards.weekly.headOption, "reward2", Period.Week),
            champion(leaderboards.monthly.headOption, "reward3", Period.Month)
          ),
          table(cls := "slist slist-pad history")(
            tbody(
              history.headOption.filter(_.date.getMonthValue != 12).map { h =>
                yearHeader(h.date.getYear)
              },
              history.map { h =>
                frag(
                  (h.date.getMonthValue == 12).option(yearHeader(h.date.getYear)),
                  tr(
                    th(h.date.getMonth.getDisplayName(java.time.format.TextStyle.FULL, ctx.lang.locale)),
                    th(userIdLink(h.champion.modId.some, withOnline = false)),
                    td(cls := "score")(h.champion.score.localize),
                    td(h.champion.action.localize),
                    td(h.champion.report.localize)
                  )
                )
              }
            )
          )
        )
      )

  def period(leaderboards: Gamify.Leaderboards, period: Gamify.Period)(using ctx: Context) =
    val title = s"Moderators of the ${period.name}"
    Page(title).css("mod.gamify"):
      main(cls := "page-menu")(
        bits.modMenu("gamify"),
        div(id := "mod-gamify", cls := "page-menu__content box")(
          boxTop(
            h1(
              a(href := routes.Mod.gamify, dataIcon := Icon.LessThan),
              title
            )
          ),
          div(cls := "period")(
            table(cls := "slist")(
              thead(
                tr(
                  th(colspan := "2"),
                  th("Actions"),
                  th("Reports"),
                  th("Score")
                )
              ),
              tbody(
                leaderboards(period).mapWithIndex: (m, i) =>
                  tr(
                    th(i + 1),
                    th(userIdLink(m.modId.some, withOnline = false)),
                    td(m.action.localize),
                    td(m.report.localize),
                    td(cls := "score")(m.score.localize)
                  )
              )
            )
          )
        )
      )

  private def champion(champ: Option[Gamify.ModMixed], img: String, period: Gamify.Period)(using Translate) =
    div(cls := "champ")(
      st.img(src := assetUrl(s"images/mod/$img.png")),
      h2("Mod of the ", period.name),
      champ
        .map { m =>
          frag(
            userIdLink(m.modId.some, withOnline = false),
            table(
              tbody(
                tr(
                  th("Total score"),
                  td(m.score)
                ),
                tr(
                  th("Actions taken"),
                  td(m.action)
                ),
                tr(
                  th("Report points"),
                  td(m.report)
                )
              )
            )
          )
        }
        .getOrElse("Nobody!"),
      a(cls := "button button-empty", href := routes.Mod.gamifyPeriod(period.name))(
        "View ",
        period.name,
        " leaderboard"
      )
    )
