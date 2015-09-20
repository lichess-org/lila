package lila.app
package templating

import lila.api.Context
import lila.team.Team

trait IRCHelper { self: TeamHelper with SecurityHelper with I18nHelper =>

  private val prompt = "1"
  private val uio = "OT10cnVlde"

  def myIrcUrl(teams: List[Team])(implicit ctx: Context) =
    """http://webchat.freenode.net?nick=%s&channels=%s&prompt=%s&uio=%s""".format(
      ctx.username | "Anon-.",
      teamChans(teams) mkString ",",
      prompt,
      uio)

  def teamChans(teams: List[Team]) = teams flatMap teamIrcChan

  def teamIrcChan(team: Team) = team.irc option "lichess-team-" + team.id
}

