package views.html
package tournament

import controllers.routes
import play.api.data.Form

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.tournament.TeamBattle
import lila.tournament.Tournament

object teamBattle:

  def edit(tour: Tournament, form: Form[?])(using PageContext) =
    views.html.base.layout(
      title = tour.name(),
      moreCss = cssTag("tournament.form"),
      moreJs = frag(
        iifeModule("vendor/textcomplete.min.js"),
        jsModule("teamBattleForm")
      )
    )(
      main(cls := "page-small")(
        div(cls := "tour__form box box-pad")(
          h1(cls := "box__top")(tour.name()),
          standardFlash,
          if tour.isFinished then p("This tournament is over, and the teams can no longer be updated.")
          else p("List the teams that will compete in this battle."),
          postForm(cls := "form3", action := routes.Tournament.teamBattleUpdate(tour.id))(
            form3.group(
              form("teams"),
              raw("One team per line. Use the auto-completion."),
              help = frag(
                "You can copy-paste this list from a tournament to another!",
                br,
                "You can't remove a team if a player has already joined the tournament with it"
              ).some
            )(
              form3.textarea(_)(rows := 10, tour.isFinished.option(disabled))
            ),
            form3.group(
              form("nbLeaders"),
              raw("Number of leaders per team. The sum of their score is the score of the team."),
              help = frag("You really shouldn't change this value after the tournament has started!").some
            )(
              form3.input(_)(tpe := "number")
            ),
            form3.globalError(form),
            form3.submit("Update teams")(tour.isFinished.option(disabled))
          )
        )
      )
    )

  private val scoreTag = tag("score")

  def standing(tour: Tournament, standing: List[TeamBattle.RankedTeam])(using PageContext) =
    views.html.base.layout(
      title = tour.name(),
      moreCss = cssTag("tournament.show.team-battle")
    )(
      main(cls := "box")(
        h1(cls := "box__top")(a(href := routes.Tournament.show(tour.id))(tour.name())),
        table(cls := "slist slist-pad tour__team-standing tour__team-standing--full")(
          tbody(
            standing.map { t =>
              tr(
                td(cls := "rank")(t.rank),
                td(cls := "team")(
                  a(href := routes.Tournament.teamInfo(tour.id, t.teamId))(teamIdToName(t.teamId))
                ),
                td(cls := "players")(
                  fragList(
                    t.leaders.map { l =>
                      scoreTag(dataHref := routes.User.show(l.userId), cls := "user-link ulpt")(l.score)
                    },
                    "+"
                  )
                ),
                td(cls := "total")(t.score)
              )
            }
          )
        )
      )
    )

  def teamInfo(tour: Tournament, team: lila.team.Team.Mini, info: TeamBattle.TeamInfo)(using
      ctx: PageContext
  ) =
    views.html.base.layout(
      title = s"${tour.name()} • ${team.name}",
      moreCss = cssTag("tournament.show.team-battle")
    )(
      main(cls := "box")(
        boxTop(
          h1(
            a(href := routes.Tournament.battleTeams(tour.id))(tour.name()),
            " • ",
            a(href := routes.Team.show(team.id))(team.name)
          )
        ),
        table(cls := "slist slist-pad")(
          tbody(
            tr(th("Players"), td(info.nbPlayers)),
            ctx.pref.showRatings option frag(
              tr(th(trans.averageElo()), td(info.avgRating)),
              tr(th(trans.arena.averagePerformance()), td(info.avgPerf))
            ),
            tr(th(trans.arena.averageScore()), td(info.avgScore))
          )
        ),
        table(cls := "slist slist-pad tour__team-info")(
          thead(
            tr(
              th(trans.rank()),
              th(trans.player()),
              th(trans.tournamentPoints()),
              ctx.pref.showRatings option th(trans.performance())
            )
          ),
          tbody(
            info.topPlayers.mapWithIndex: (player, index) =>
              tr(
                td(index + 1),
                td(
                  (index < tour.teamBattle.so(_.nbLeaders)) option iconTag(licon.Crown),
                  userIdLink(player.userId.some)
                ),
                td(player.score),
                ctx.pref.showRatings option td(player.performance)
              )
          )
        )
      )
    )
