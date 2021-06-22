package lila.irc

import org.joda.time.DateTime

import lila.common.{ ApiVersion, EmailAddress, IpAddress, LightUser }
import lila.user.User
import lila.user.Holder

final class SlackApi(
    client: SlackClient,
    zulip: ZulipClient,
    implicit val lightUser: LightUser.Getter
)(implicit ec: scala.concurrent.ExecutionContext) {

  import SlackApi._

  def logMod(modId: User.ID, icon: String, text: String): Funit =
    lightUser(modId) flatMap {
      _ ?? { mod =>
        client(
          SlackMessage(
            username = mod.name,
            icon = "scroll",
            text = s":$icon: ${linkifyUsers(text)}",
            channel = rooms.tavernLog
          )
        )
      }
    }

  def printBan(mod: Holder, print: String, userIds: List[User.ID]): Funit =
    logMod(mod.id, "footprints", s"Ban print $print of ${userIds} users: ${userIds map linkifyUsers}")

  def chatPanic(mod: Holder, v: Boolean): Funit =
    client(
      SlackMessage(
        username = mod.user.username,
        icon = if (v) "anger" else "information_source",
        text = s"${if (v) "Enabled" else "Disabled"} $chatPanicLink",
        channel = rooms.tavern
      )
    )

  def garbageCollector(message: String): Funit =
    client(
      SlackMessage(
        username = "Garbage Collector",
        icon = "put_litter_in_its_place",
        text = linkifyUsers(message),
        channel = rooms.tavernBots
      )
    )

  def broadcastError(id: String, name: String, error: String): Funit =
    client(
      SlackMessage(
        username = "lichess error",
        icon = "lightning",
        text = s"${broadcastLink(id, name)}: $error",
        channel = rooms.broadcast
      )
    )

  private def link(url: String, name: String)         = s"<$url|$name>"
  private def lichessLink(path: String, name: String) = s"<https://lichess.org$path|$name>"
  private def userLink(name: String): String          = lichessLink(s"/@/$name?mod", name)
  private def userLink(user: User): String            = userLink(user.username)
  private def gameLink(id: String)                    = lichessLink(s"/$id", s"#$id")
  private def broadcastLink(id: String, name: String) = lichessLink(s"/broadcast/-/$id", name)
  private val chatPanicLink                           = lichessLink("mod/chat-panic", "Chat Panic")

  private val userRegex   = lila.common.String.atUsernameRegex.pattern
  private val userReplace = link("https://lichess.org/@/$1?mod", "$1")

  private def linkifyUsers(msg: String) =
    userRegex matcher msg replaceAll userReplace

  def userAppeal(user: User, mod: Holder): Funit =
    client(
      SlackMessage(
        username = mod.user.username,
        icon = "eyes",
        text =
          s"Let's have a look at the appeal of _*${lichessLink(s"/appeal/${user.username}", user.username)}*_",
        channel = rooms.tavernAppeal
      )
    )

  def stop(): Funit =
    client(
      SlackMessage(
        username = "deployment",
        icon = "lichess",
        text = "Lichess is being updated! Brace for impact.",
        channel = rooms.general
      )
    )
}

object SlackApi {

  private[irc] object rooms {
    val general                         = "team"
    val tavern                          = "tavern"
    val tavernBots                      = "tavern-bots"
    val tavernNotes                     = "tavern-notes"
    val tavernAppeal                    = "tavern-appeal"
    val tavernLog                       = "tavern-log"
    val broadcast                       = "broadcast"
    def tavernMonitor(tpe: MonitorType) = s"tavern-monitor-${tpe.toString.toLowerCase}"
    val tavernMonitorAll                = "tavern-monitor-all"
    val gdprLog                         = "gdpr-log"
  }

  private[irc] object stage {
    val name = "stage.lichess.org"
    val icon = "volcano"
  }
}
