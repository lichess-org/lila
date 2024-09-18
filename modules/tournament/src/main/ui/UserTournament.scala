package lila.tournament
package ui

import scalalib.paginator.Paginator

import lila.core.i18n.Translate
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class UserTournament(helpers: Helpers, ui: TournamentUi):
  import helpers.{ *, given }

  def best(u: User, pager: Paginator[LeaderboardApi.TourEntry])(using Context) =
    page(u, s"${u.username} best tournaments", "best").js(infiniteScrollEsmInit):
      list(u, "best", pager, "BEST")

  def recent(u: User, pager: Paginator[LeaderboardApi.TourEntry])(using Context) =
    page(u, s"${u.username} recent tournaments", "recent").js(infiniteScrollEsmInit):
      list(u, "recent", pager, pager.nbResults.toString)

  def created(u: User, pager: Paginator[lila.tournament.Tournament])(using Context) =
    page(u, s"${u.username} created tournaments", "created")
      .js(infiniteScrollEsmInit):
        if pager.nbResults == 0 then div(cls := "box-pad")(trans.site.nothingToSeeHere())
        else
          div(cls := "tournament-list")(
            table(cls := "slist")(
              thead(
                tr(
                  th(cls := "count")(pager.nbResults),
                  th(colspan := 2)(h1(frag(userLink(u, withOnline = true), " • ", trans.site.tournaments()))),
                  th(trans.site.winner()),
                  th(trans.site.players())
                )
              ),
              tbody(cls := "infinite-scroll")(
                pager.currentPageResults.map { t =>
                  tr(cls := "paginated")(
                    td(cls := "icon")(iconTag(ui.tournamentIcon(t))),
                    ui.finishedList.header(t),
                    td(momentFromNow(t.startsAt)),
                    td(cls := "winner")(
                      t.winnerId.isDefined.option(userIdLink(t.winnerId, withOnline = false))
                    ),
                    td(cls := "text", dataIcon := Icon.User)(t.nbPlayers.localize)
                  )
                },
                pagerNextTable(pager, np => routes.UserTournament.path(u.username, "created", np).url)
              )
            )
          )

  def upcoming(u: User, pager: Paginator[lila.tournament.Tournament])(using Context) =
    page(u, s"${u.username} upcoming tournaments", "upcoming"):
      if pager.nbResults == 0 then div(cls := "box-pad")(trans.site.nothingToSeeHere())
      else
        div(cls := "tournament-list")(
          table(cls := "slist")(
            thead(
              tr(
                th(cls := "count")(pager.nbResults),
                th(colspan := 2)(
                  h1(frag(userLink(u, withOnline = true)), " • ", trans.team.upcomingTournaments())
                ),
                th(trans.site.players())
              )
            ),
            tbody:
              pager.currentPageResults.map: t =>
                tr(
                  td(cls := "icon")(iconTag(ui.tournamentIcon(t))),
                  ui.finishedList.header(t),
                  td(momentFromNow(t.startsAt)),
                  td(cls := "text", dataIcon := Icon.User)(t.nbPlayers.localize)
                )
          )
        )

  def chart(u: User, data: lila.tournament.LeaderboardApi.ChartData)(using Context) =
    page(u, title = s"${u.username} • ${trans.arena.tournamentStats.txt()}", path = "chart"):
      div(cls := "tournament-stats")(
        boxTop(h1(frag(userLink(u, withOnline = true), " • ", trans.arena.tournamentStats()))),
        p(cls := "box__pad")(trans.arena.rankAvgHelp()),
        p(cls := "box__pad")(
          trans.arena.allAveragesAreX:
            a(href := "https://www.dictionary.com/e/average-vs-mean-vs-median-vs-mode")(trans.arena.medians())
        ),
        table(cls := "slist slist-pad perf-results")(
          thead(
            tr(
              th,
              th(trans.site.tournaments()),
              th(trans.arena.pointsAvg()),
              th(trans.arena.pointsSum()),
              th(trans.arena.rankAvg())
            )
          ),
          tbody(
            data.perfResults.map { case (pt, res) =>
              tr(
                th(iconTag(pt.icon, pt.trans)),
                td(res.nb.localize),
                td(res.points.median.map(_.toInt)),
                td(res.points.sum.localize),
                td(res.rankPercentMedian, "%")
              )
            },
            tr(
              th(trans.arena.total()),
              td(data.allPerfResults.nb.localize),
              td(data.allPerfResults.points.median.map(_.toInt)),
              td(data.allPerfResults.points.sum.localize),
              td(data.allPerfResults.rankPercentMedian, "%")
            )
          )
        )
      )

  private def list(
      u: User,
      path: String,
      pager: Paginator[LeaderboardApi.TourEntry],
      count: String
  )(using Translate) =
    if pager.nbResults == 0 then div(cls := "box-pad")(trans.site.nothingToSeeHere())
    else
      div(cls := "tournament-list")(
        table(cls := "slist")(
          thead(
            tr(
              th(cls := "count")(count),
              th(h1(frag(userLink(u, withOnline = true), " • ", trans.site.tournaments()))),
              th(trans.site.games()),
              th(trans.site.points()),
              th(trans.site.rank())
            )
          ),
          tbody(cls := "infinite-scroll")(
            pager.currentPageResults.map { e =>
              tr(cls := List("paginated" -> true, "scheduled" -> e.tour.isScheduled))(
                td(cls := "icon")(iconTag(ui.tournamentIcon(e.tour))),
                td(cls := "header")(
                  a(href := routes.Tournament.show(e.tour.id))(
                    span(cls := "name")(e.tour.name()),
                    span(cls := "setup")(
                      e.tour.clock.show,
                      " • ",
                      if e.tour.variant.exotic then e.tour.variant.name else e.tour.perfType.trans,
                      " • ",
                      momentFromNow(e.tour.startsAt)
                    )
                  )
                ),
                td(cls := "games")(e.entry.nbGames),
                td(cls := "score")(e.entry.score),
                td(cls := "rank")(strong(e.entry.rank), " / ", e.tour.nbPlayers)
              )
            },
            pagerNextTable(pager, np => routes.UserTournament.path(u.username, path, np).url)
          )
        )
      )

  private def page(u: User, title: String, path: String)(using ctx: Context) =
    Page(title)
      .css("bits.user-tournament")
      .wrap: body =>
        main(cls := "page-menu")(
          lila.ui.bits.pageMenuSubnav(
            a(cls := path.active("created"), href := routes.UserTournament.path(u.username, "created"))(
              trans.arena.created()
            ),
            ctx
              .is(u)
              .option(
                a(cls := path.active("upcoming"), href := routes.UserTournament.path(u.username, "upcoming"))(
                  trans.broadcast.upcoming()
                )
              ),
            a(cls := path.active("recent"), href := routes.UserTournament.path(u.username, "recent"))(
              trans.arena.recentlyPlayed()
            ),
            a(cls := path.active("best"), href := routes.UserTournament.path(u.username, "best"))(
              trans.arena.bestResults()
            ),
            a(cls := path.active("chart"), href := routes.UserTournament.path(u.username, "chart"))(
              trans.arena.stats()
            )
          ),
          div(cls := "page-menu__content box")(body)
        )
