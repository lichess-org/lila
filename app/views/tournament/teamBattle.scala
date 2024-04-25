package views.html
package tournament

import play.api.data.Form

import lila.app.templating.Environment.{ *, given }

import lila.tournament.{ TeamBattle, Tournament }
import lila.core.team.LightTeam

object teamBattle:

  private lazy val ui = lila.tournament.ui.TeamBattleUi(helpers)

  def edit(tour: Tournament, form: Form[?])(using PageContext) =
    views.html.base.layout(
      title = tour.name(),
      moreCss = cssTag("tournament.form"),
      modules = jsModule("bits.teamBattleForm")
    )(ui.edit(tour, form))

  def standing(tour: Tournament, standing: List[TeamBattle.RankedTeam])(using PageContext) =
    views.html.base.layout(
      title = tour.name(),
      moreCss = cssTag("tournament.show.team-battle")
    )(ui.standing(tour, standing))

  def teamInfo(tour: Tournament, team: LightTeam, info: TeamBattle.TeamInfo)(using
      ctx: PageContext
  ) =
    views.html.base.layout(
      title = s"${tour.name()} • ${team.name}",
      moreCss = cssTag("tournament.show.team-battle")
    )(ui.teamInfo(tour, team, info))
