package lila.app
package templating

import lila.user.Context

trait IRCHelper { self: TeamHelper with SecurityHelper with I18nHelper ⇒

  private val prompt = "1"
  private val uio = "OT10cnVlde"

  def myIrcUrl(implicit ctx: Context) =
    """http://webchat.freenode.net?nick=%s&channels=%s&prompt=%s&uio=%s""".format(
      ctx.username | "Anon-.",
      myIrcChannels mkString ",",
      prompt,
      uio)

  def myIrcChannels(implicit ctx: Context): List[String] =
    teamChans ::: staffChans ::: langChans

  private def teamChans(implicit ctx: Context) = ctx.userId ?? { userId ⇒
    teamIds(userId) map { "lichess-team-" + _ }
  }

  private def staffChans(implicit ctx: Context) =
    isGranted(_.StaffForum) ?? List("lichess-staff")

  private def langChans(implicit ctx: Context) =
    List("lichess", lang.language + ".lichess")
}
