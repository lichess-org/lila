package lila.irc

import org.joda.time.DateTime

import lila.common.IpAddress
import lila.common.{ ApiVersion, EmailAddress, Heapsort, IpAddress, LightUser }
import lila.hub.actorApi.irc._
import lila.user.Holder
import lila.user.User

final class IrcApi(
    zulip: ZulipClient,
    noteApi: lila.user.NoteApi,
    implicit val lightUser: LightUser.Getter
)(implicit ec: scala.concurrent.ExecutionContext) {

  import IrcApi._

  def commReportBurst(user: User): Funit = {
    val md = markdown.linkifyUsers(s"Burst of comm reports about @${user.username}")
    zulip(_.mod.commsPrivate, "burst")(md)
  }

  def userMod(user: User, mod: Holder): Funit =
    noteApi
      .forMod(user.id)
      .map(_.headOption.filter(_.date isAfter DateTime.now.minusMinutes(5)))
      .flatMap {
        case None =>
          zulip(_.mod.hunterCheat, user.username)(
            s":eyes: ${markdown.userLink(mod.user.username)}: Let's have a look at **${markdown.userLink(user.username)}**"
          )
        case Some(note) =>
          zulip(_.mod.hunterCheat, user.username)(
            s"${markdown.modLink(mod.user.username)} :note: **${markdown
              .userLink(user.username)}** (${markdown.userNotesLink(user.username)}):\n" +
              markdown.linkifyUsers(note.text take 2000)
          )
      }

  def userModNote(modName: String, username: String, note: String): Funit =
    zulip(_.mod.adminLog, "notes")(
      s"${markdown.modLink(modName)} :note: **${markdown.userLink(username)}** (${markdown.userNotesLink(username)}):\n" +
        markdown.linkifyUsers(note take 2000)
    )

  def selfReport(typ: String, path: String, user: User, ip: IpAddress): Funit =
    zulip(_.mod.adminLog, "self report")(
      s"[**$typ**] ${markdown.userLink(user)}@$ip ${markdown.gameLink(path)}"
    )

  def commlog(mod: Holder, user: User, reportBy: Option[User.ID]): Funit =
    zulip(_.mod.adminLog, "private comms checks")({
      val finalS = if (user.username endsWith "s") "" else "s"
      s"**${markdown modLink mod.user.username}** checked out **${markdown userLink user.username}**'$finalS communications "
    } + reportBy.filter(mod.id !=).fold("spontaneously") { by =>
      s"while investigating a report created by ${markdown.userLink(by)}"
    })

  def monitorMod(modId: User.ID, icon: String, text: String, tpe: MonitorType): Funit =
    lightUser(modId) flatMap {
      _ ?? { mod =>
        val md = s"${markdown.userLink(mod.name)} :$icon: ${markdown.linkifyUsers(text)}"
        zulip(_.mod.adminMonitor(tpe), mod.name)(md) >>
          zulip(_.mod.adminMonitorAll, mod.name)(md)
      }
    }

  def logMod(modId: User.ID, icon: String, text: String): Funit =
    lightUser(modId) flatMap {
      _ ?? { mod =>
        zulip(_.mod.log, "actions")(
          s"${markdown.modLink(modId)} :$icon: ${markdown.linkifyUsers(text)}"
        )
      }
    }

  // def printBan(mod: Holder, print: String, userIds: List[User.ID]): Funit =
  //   logMod(mod.id, "footprints", s"Ban print $print of ${userIds} users: ${userIds map linkifyUsers}")

  def chatPanic(mod: Holder, v: Boolean): Funit =
    zulip(_.mod.log, "chat panic")(
      s":stop: ${markdown.modLink(mod.user)} ${if (v) "enabled" else "disabled"} ${markdown.lichessLink("mod/chat-panic", " Chat Panic")}"
    )

  def garbageCollector(msg: String): Funit =
    zulip(_.mod.log, "garbage collector")(markdown linkifyUsers msg)

  def broadcastError(id: String, name: String, error: String): Funit =
    zulip(_.broadcast, "lila error log")(s"${markdown.broadcastLink(id, name)} $error")

  def userAppeal(user: User, mod: Holder): Funit =
    zulip(_.mod.adminAppeal, user.username)(
      s"${markdown.modLink(mod.user)} :eyes: Let's have a look at the appeal of **${markdown
        .lichessLink(s"/appeal/${user.username}", user.username)}**"
    )

  def stop(): Funit = zulip(_.general, "lila")("Lichess is restarting.")

  def publishEvent(event: Event): Funit = event match {
    case Error(msg)   => publishError(msg)
    case Warning(msg) => publishWarning(msg)
    case Info(msg)    => publishInfo(msg)
    case Victory(msg) => publishVictory(msg)
  }

  private def publishError(msg: String): Funit =
    zulip(_.general, "lila")(s":lightning: ${markdown linkifyUsers msg}")

  private def publishWarning(msg: String): Funit =
    zulip(_.general, "lila")(s":thinking: ${markdown linkifyUsers msg}")

  private def publishVictory(msg: String): Funit =
    zulip(_.general, "lila")(s":tada: ${markdown linkifyUsers msg}")

  private[irc] def publishInfo(msg: String): Funit =
    zulip(_.general, "lila")(s":info: ${markdown linkifyUsers msg}")

  object charge {
    import lila.hub.actorApi.plan.ChargeEvent
    private var buffer: Vector[ChargeEvent] = Vector.empty
    implicit private val amountOrdering     = Ordering.by[ChargeEvent, Int](_.cents)

    def apply(event: ChargeEvent): Funit = {
      buffer = buffer :+ event
      buffer.head.date.isBefore(DateTime.now.minusHours(12)) ?? {
        val firsts    = Heapsort.topN(buffer, 10, amountOrdering).map(_.username).map(userAt).mkString(", ")
        val amountSum = buffer.map(_.cents).sum
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
      zulip(_.general, "lila")(markdown.linkifyUsers(text))

    private def userAt(username: String) =
      if (username == "Anonymous") "Anonymous"
      else s"@$username"

    private def amount(cents: Int) = s"$$${BigDecimal(cents.toLong, 2)}"
  }
}

object IrcApi {

  sealed trait MonitorType {
    def key = toString.toLowerCase
  }
  object MonitorType {
    case object Hunt  extends MonitorType
    case object Comm  extends MonitorType
    case object Other extends MonitorType
  }

  private val userRegex = lila.common.String.atUsernameRegex.pattern

  private object markdown {
    def link(url: String, name: String)         = s"[$name]($url)"
    def lichessLink(path: String, name: String) = s"[$name](https://lichess.org$path)"
    def userLink(name: String): String          = lichessLink(s"/@/$name?mod", name)
    def userLink(user: User): String            = userLink(user.username)
    def modLink(name: String): String           = lichessLink(s"/@/$name", name)
    def modLink(user: User): String             = modLink(user.username)
    def gameLink(id: String)                    = lichessLink(s"/$id", s"#$id")
    def userNotesLink(name: String)             = lichessLink(s"/@/$name?notes", "notes")
    def broadcastLink(id: String, name: String) = lichessLink(s"/broadcast/-/$id", name)
    val userReplace                             = link("https://lichess.org/@/$1?mod", "$1")
    def linkifyUsers(msg: String)               = userRegex matcher msg replaceAll userReplace
  }
}
