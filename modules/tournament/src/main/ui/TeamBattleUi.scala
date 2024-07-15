package lila.tournament
package ui

import play.api.data.Form

import lila.core.team.LightTeam
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class TeamBattleUi(helpers: Helpers):
  import helpers.{ *, given }

  private val scoreTag = tag("score")

  def edit(tour: Tournament, form: Form[?])(using Context) =
    Page(tour.name())
      .css("tournament.form")
      .js(EsmInit("bits.teamBattleForm")):
        main(cls := "page-small")(
          div(cls := "tour__form box box-pad")(
            h1(cls := "box__top")(tour.name()),
            standardFlash,
            if tour.isFinished then p(trans.team.thisTeamBattleIsOver())
            else p(trans.team.listTheTeamsThatWillCompete()),
            postForm(cls := "form3", action := routes.Tournament.teamBattleUpdate(tour.id))(
              form3.group(
                form("teams"),
                trans.team.oneTeamPerLine(),
                help = trans.team.oneTeamPerLineHelp().some
              )(
                form3.textarea(_)(rows := 10, tour.isFinished.option(disabled))
              ),
              form3.group(
                form("nbLeaders"),
                trans.team.numberOfLeadsPerTeam(),
                help = trans.team.numberOfLeadsPerTeamHelp().some
              )(
                form3.input(_)(tpe := "number")
              ),
              form3.globalError(form),
              form3.submit(trans.site.save())(tour.isFinished.option(disabled))
            )
          )
        )

  def standing(tour: Tournament, standing: List[TeamBattle.RankedTeam])(using Context) =
    Page(tour.name())
      .css("tournament.show.team-battle"):
        main(cls := "box")(
          h1(cls := "box__top")(a(href := routes.Tournament.show(tour.id))(tour.name())),
          table(cls := "slist slist-pad tour__team-standing tour__team-standing--full")(
            tbody(
              standing.map: t =>
                val team = teamIdToLight(t.teamId)
                tr(
                  td(cls := "rank")(t.rank),
                  td(cls := "team"):
                    a(href := routes.Tournament.teamInfo(tour.id, team.id))(team.name, teamFlair(team))
                  ,
                  td(cls := "players")(
                    fragList(
                      t.leaders.map: l =>
                        scoreTag(dataHref := routes.User.show(l.userId), cls := "user-link ulpt")(l.score),
                      "+"
                    )
                  ),
                  td(cls := "total")(t.score)
                )
            )
          )
        )

  def teamInfo(tour: Tournament, team: LightTeam, info: TeamBattle.TeamInfo)(using ctx: Context) =
    Page(s"${tour.name()} • ${team.name}")
      .css("tournament.show.team-battle"):
        main(cls := "box")(
          boxTop(
            h1(
              a(href := routes.Tournament.battleTeams(tour.id))(tour.name()),
              hr,
              teamLink(team, true)
            )
          ),
          table(cls := "slist slist-pad")(
            tbody(
              tr(th("Players"), td(info.nbPlayers)),
              ctx.pref.showRatings.option(
                frag(
                  tr(th(trans.site.averageElo()), td(info.avgRating)),
                  tr(th(trans.arena.averagePerformance()), td(info.avgPerf))
                )
              ),
              tr(th(trans.arena.averageScore()), td(info.avgScore))
            )
          ),
          table(cls := "slist slist-pad tour__team-info")(
            thead(
              tr(
                th(trans.site.rank()),
                th(trans.site.player()),
                th(trans.site.tournamentPoints()),
                ctx.pref.showRatings.option(th(trans.site.performance()))
              )
            ),
            tbody(
              info.topPlayers.mapWithIndex: (player, index) =>
                tr(
                  td(index + 1),
                  td(
                    (index < tour.teamBattle.so(_.nbLeaders)).option(iconTag(Icon.Crown)),
                    userIdLink(player.userId.some)
                  ),
                  td(player.score),
                  ctx.pref.showRatings.option(td(player.performance))
                )
            )
          )
        )
