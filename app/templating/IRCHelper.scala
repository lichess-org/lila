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
      myIrcChannels(teams) mkString ",",
      prompt,
      uio)

  def myIrcChannels(teams: List[Team])(implicit ctx: Context): List[String] =
    teamChans(teams) ::: staffChans ::: langChans

  private def teamChans(teams: List[Team]) = teams.map(teamIrcChan).flatten

  def teamIrcChan(team: Team) = team.irc option "lichess-team-" + team.id

  private def staffChans(implicit ctx: Context) =
    isGranted(_.StaffForum) ?? List("lichess-staff")

  private def langChans(implicit ctx: Context) =
    List("lichess", lang.language + ".lichess") filterNot ("en.lichess" ==)
}

