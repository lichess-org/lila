package lila.irc

import org.joda.time.DateTime

import lila.common.{ ApiVersion, EmailAddress, Heapsort, IpAddress, LightUser }
import lila.user.User

final class DiscordApi(
    client: DiscordClient,
    implicit val lightUser: LightUser.Getter
)(implicit ec: scala.concurrent.ExecutionContext) {

  import DiscordApi._

  def commReportBurst(user: User): Funit =
    client(
      DiscordMessage(
        text = linkifyUsers(s"Burst of comm reports about @${user.username}"),
        channel = channels.comms
      )
    )

  private def link(url: String, name: String)         = s"[$url]($name)"
  private def lichessLink(path: String, name: String) = s"[https://lichess.org$path]($name)"
  private def userLink(name: String): String          = lichessLink(s"/@/$name?mod", name)
  private def userLink(user: User): String            = userLink(user.username)

  private val userRegex   = lila.common.String.atUsernameRegex.pattern
  private val userReplace = link("https://lichess.org/@/$1?mod", "$1")

  private def linkifyUsers(msg: String) =
    userRegex matcher msg replaceAll userReplace
}

private object DiscordApi {

  object channels {
    val webhook = 672938635120869400d
    val comms   = 685084348096970770d
  }
}
