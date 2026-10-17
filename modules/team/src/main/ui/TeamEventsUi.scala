package lila.team
package ui

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }

final class TeamEventsUi(helpers: Helpers, bits: TeamUi):
  import helpers.{ *, given }
  import trans.team as trt
  import bits.{ TeamPage, menu }

  def events(
      t: Team.WithLeaders,
      addLeaderForm: Form[UserStr],
      permsForm: Form[List[TeamSecurity.LeaderData]]
  )(using Context) =
    TeamPage(s"${t.name} • ${trans.team.teamLeaders.txt()}")
