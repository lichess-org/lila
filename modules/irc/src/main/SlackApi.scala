package lila.irc

import org.joda.time.DateTime

import lila.common.{ ApiVersion, EmailAddress, Heapsort, IpAddress, LightUser }
import lila.hub.actorApi.slack._
import lila.user.User
import lila.user.Holder

final class SlackApi(
    client: SlackClient,
    noteApi: lila.user.NoteApi,
    implicit val lightUser: LightUser.Getter
)(implicit ec: scala.concurrent.ExecutionContext) {

  import SlackApi._

  object charge {

    import lila.hub.actorApi.plan.ChargeEvent

    private var buffer: Vector[ChargeEvent] = Vector.empty

    implicit private val amountOrdering = Ordering.by[ChargeEvent, Int](_.amount)

    def apply(event: ChargeEvent): Funit = {
      buffer = buffer :+ event
      buffer.head.date.isBefore(DateTime.now.minusHours(12)) ?? {
        val firsts    = Heapsort.topN(buffer, 10, amountOrdering).map(_.username).map(userAt).mkString(", ")
        val amountSum = buffer.map(_.amount).sum
        val patrons =
          if (firsts.lengthIs > 10) s"$firsts and, like, ${firsts.length - 10} others,"
          else firsts
        displayMessage {
          s"$patrons donated ${amount(amountSum)}. Monthly progress: ${buffer.last.percent}%"
        } >>- {
          buffer = Vector.empty
        }
      }
    }

    private def displayMessage(text: String) =
      client(
        SlackMessage(
          username = "Patron",
          icon = "four_leaf_clover",
          text = linkifyUsers(text),
          channel = "team"
        )
      )

    private def userAt(username: String) =
      if (username == "Anonymous") "Anonymous"
      else s"@$username"

    private def amount(cents: Int) = s"$$${BigDecimal(cents.toLong, 2)}"
  }

  def publishEvent(event: Event): Funit =
    event match {
      case Error(msg)   => publishError(msg)
      case Warning(msg) => publishWarning(msg)
      case Info(msg)    => publishInfo(msg)
      case Victory(msg) => publishVictory(msg)
      case TournamentName(userName, tourId, tourName) =>
        client(
          SlackMessage(
            username = "Tournament name alert",
            icon = "children_crossing",
            text =
              s"${userLink(userName)} created ${link(s"https://lichess.org/tournament/$tourId", s"$tourName Arena")}",
            channel = rooms.tavern
          )
        )
    }

  def commlog(mod: Holder, user: User, reportBy: Option[User.ID]): Funit =
    client(
      SlackMessage(
        username = mod.user.username,
        icon = "eye",
        text = {
          val finalS = if (user.username endsWith "s") "" else "s"
          s"checked out _*${userLink(user.username)}*_'$finalS communications "
        } + reportBy.filter(mod.id !=).fold("spontaneously") { by =>
          s"while investigating a report created by ${userLink(by)}"
        },
        channel = "commlog"
      )
    )

  def monitorMod(modId: User.ID, icon: String, text: String, monitorType: MonitorType): Funit =
    lightUser(modId) flatMap {
      _ ?? { mod =>
        val msg =
          SlackMessage(
            username = mod.name,
            icon = "scroll",
            text = s":$icon: ${linkifyUsers(text)}",
            channel = rooms.tavernMonitor(monitorType)
          )
        client(msg) >> client(msg.copy(channel = rooms.tavernMonitorAll))
      }
    }

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

  def selfReport(typ: String, path: String, user: User, ip: IpAddress): Funit =
    client(
      SlackMessage(
        username = "Self Report",
        icon = "kms",
        text = s"[*$typ*] ${userLink(user)}@$ip ${gameLink(path)}",
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

  def publishError(msg: String): Funit =
    client(
      SlackMessage(
        username = "lichess error",
        icon = "lightning",
        text = linkifyUsers(msg),
        channel = rooms.general
      )
    )

  def publishWarning(msg: String): Funit =
    client(
      SlackMessage(
        username = "lichess warning",
        icon = "thinking_face",
        text = linkifyUsers(msg),
        channel = rooms.general
      )
    )

  def publishVictory(msg: String): Funit =
    client(
      SlackMessage(
        username = "lichess victory",
        icon = "tada",
        text = linkifyUsers(msg),
        channel = rooms.general
      )
    )

  def publishInfo(msg: String): Funit =
    client(
      SlackMessage(
        username = "lichess info",
        icon = "lichess",
        text = linkifyUsers(msg),
        channel = rooms.general
      )
    )

  def gdprErase(user: User): Funit =
    client(
      SlackMessage(
        username = user.username,
        icon = "scream2",
        text = "GDPR erasure scheduled",
        channel = rooms.gdprLog
      )
    )

  private def link(url: String, name: String)         = s"<$url|$name>"
  private def lichessLink(path: String, name: String) = s"<https://lichess.org$path|$name>"
  private def userLink(name: String): String          = lichessLink(s"/@/$name?mod", name)
  private def userLink(user: User): String            = userLink(user.username)
  private def gameLink(id: String)                    = lichessLink(s"/$id", s"#$id")
  private def userNotesLink(name: String)             = lichessLink(s"/@/$name?notes", "notes")
  private def broadcastLink(id: String, name: String) = lichessLink(s"/broadcast/-/$id", name)
  private val chatPanicLink                           = lichessLink("mod/chat-panic", "Chat Panic")

  private val userRegex   = lila.common.String.atUsernameRegex.pattern
  private val userReplace = link("https://lichess.org/@/$1?mod", "$1")

  private def linkifyUsers(msg: String) =
    userRegex matcher msg replaceAll userReplace

  def userMod(user: User, mod: Holder): Funit =
    noteApi
      .forMod(user.id)
      .map(_.headOption.filter(_.date isAfter DateTime.now.minusMinutes(5)))
      .map {
        case None =>
          SlackMessage(
            username = mod.user.username,
            icon = "eyes",
            text = s"Let's have a look at _*${userLink(user.username)}*_",
            channel = rooms.tavern
          )
        case Some(note) =>
          SlackMessage(
            username = mod.user.username,
            icon = "spiral_note_pad",
            text = s"_*${userLink(user.username)}*_ (${userNotesLink(user.username)}):\n" +
              linkifyUsers(note.text take 2000),
            channel = rooms.tavern
          )
      } flatMap client.apply

  def userModNote(modName: String, username: String, note: String): Funit =
    client(
      SlackMessage(
        username = modName,
        icon = "spiral_note_pad",
        text = (s"_*${userLink(username)}*_ (${userNotesLink(username)}):\n" +
          linkifyUsers(note take 2000)),
        channel = rooms.tavernNotes
      )
    )

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

  sealed trait MonitorType
  object MonitorType {
    case object Hunt  extends MonitorType
    case object Comm  extends MonitorType
    case object Other extends MonitorType
  }

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
