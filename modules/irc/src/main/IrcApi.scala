package lila.irc

import org.joda.time.DateTime

import lila.common.IpAddress
import lila.common.{ ApiVersion, EmailAddress, Heapsort, IpAddress, LightUser }
import lila.hub.actorApi.irc._
import lila.user.Holder
import lila.user.User

final class IrcApi(
    discord: DiscordClient,
    slack: SlackClient,
    zulip: ZulipClient,
    noteApi: lila.user.NoteApi,
    implicit val lightUser: LightUser.Getter
)(implicit ec: scala.concurrent.ExecutionContext) {

  import IrcApi._

  def commReportBurst(user: User): Funit = {
    val md = markdown.linkifyUsers(s"Burst of comm reports about @${user.username}")
    discord.comms(md) >> zulip.mod()(md)
  }

  def userMod(user: User, mod: Holder): Funit =
    noteApi
      .forMod(user.id)
      .map(_.headOption.filter(_.date isAfter DateTime.now.minusMinutes(5)))
      .flatMap {
        case None =>
          slack(
            SlackMessage(
              username = mod.user.username,
              icon = "eyes",
              text = s"Let's have a look at _*${slackdown.userLink(user.username)}*_",
              channel = SlackClient.rooms.tavern
            )
          ) >> zulip.mod()(
            s":eyes: ${markdown.userLink(mod.user.username)}: Let's have a look at **${markdown.userLink(user.username)}**"
          )
        case Some(note) =>
          slack(
            SlackMessage(
              username = mod.user.username,
              icon = "spiral_note_pad",
              text =
                s"_*${slackdown.userLink(user.username)}*_ (${slackdown.userNotesLink(user.username)}):\n" +
                  slackdown.linkifyUsers(note.text take 2000),
              channel = SlackClient.rooms.tavern
            )
          ) >> zulip.mod()(
            s"${markdown.userLink(mod.user.username)} :note: **${markdown
              .userLink(user.username)}** (${markdown.userNotesLink(user.username)}):\n" +
              markdown.linkifyUsers(note.text take 2000)
          )
      }

  def userModNote(modName: String, username: String, note: String): Funit =
    slack(
      SlackMessage(
        username = modName,
        icon = "spiral_note_pad",
        text = s"_*${slackdown.userLink(username)}*_ (${slackdown.userNotesLink(username)}):\n" +
          slackdown.linkifyUsers(note take 2000),
        channel = SlackClient.rooms.tavernNotes
      )
    ) >>
      zulip.mod(ZulipClient.topic.notes)(
        s":note: ${markdown.userLink(modName)} **${markdown.userLink(username)}** (${markdown.userNotesLink(username)}):\n" +
          markdown.linkifyUsers(note take 2000)
      )

  def selfReport(typ: String, path: String, user: User, ip: IpAddress): Funit =
    slack(
      SlackMessage(
        username = "Self Report",
        icon = "kms",
        text = s"[*$typ*] ${slackdown.userLink(user)}@$ip ${slackdown.gameLink(path)}",
        channel = SlackClient.rooms.tavernBots
      )
    ) >> zulip.mod(ZulipClient.topic.clientReports)(
      s"[*$typ*] ${markdown.userLink(user)}@$ip ${markdown.gameLink(path)}"
    )

  def commlog(mod: Holder, user: User, reportBy: Option[User.ID]): Funit =
    slack(
      SlackMessage(
        username = mod.user.username,
        icon = "eye",
        text = {
          val finalS = if (user.username endsWith "s") "" else "s"
          s"checked out _*${slackdown.userLink(user.username)}*_'$finalS communications "
        } + reportBy.filter(mod.id !=).fold("spontaneously") { by =>
          s"while investigating a report created by ${slackdown.userLink(by)}"
        },
        channel = "commlog"
      )
    ) >> zulip.mod(ZulipClient.topic.commLog)({
      val finalS = if (user.username endsWith "s") "" else "s"
      s"**${markdown userLink mod.user.username}** checked out **${markdown userLink user.username}**'$finalS communications "
    } + reportBy.filter(mod.id !=).fold("spontaneously") { by =>
      s"while investigating a report created by ${markdown.userLink(by)}"
    })

  def monitorMod(modId: User.ID, icon: String, text: String, tpe: MonitorType): Funit =
    lightUser(modId) flatMap {
      _ ?? { mod =>
        val msg =
          SlackMessage(
            username = mod.name,
            icon = "scroll",
            text = s":$icon: ${slackdown.linkifyUsers(text)}",
            channel = s"tavern-monitor-${tpe.toString.toLowerCase}"
          )
        val md = s"${markdown.userLink(mod.name)} :$icon: ${markdown.linkifyUsers(text)}"
        slack(msg) >>
          slack(msg.copy(channel = SlackClient.rooms.tavernMonitorAll)) >>
          zulip.mod(s"monitor-${tpe.toString.toLowerCase}")(md) >>
          zulip.mod(ZulipClient.topic.monitor)(md)
      }
    }

  def logMod(modId: User.ID, icon: String, text: String): Funit =
    lightUser(modId) flatMap {
      _ ?? { mod =>
        slack(
          SlackMessage(
            username = mod.name,
            icon = "scroll",
            text = s":$icon: ${slackdown.linkifyUsers(text)}",
            channel = SlackClient.rooms.tavernLog
          )
        ) >>
          zulip.mod(ZulipClient.topic.actionLog)(
            s"${markdown.userLink(modId)} :$icon: ${markdown.linkifyUsers(text)}"
          )
      }
    }

  // def printBan(mod: Holder, print: String, userIds: List[User.ID]): Funit =
  //   logMod(mod.id, "footprints", s"Ban print $print of ${userIds} users: ${userIds map linkifyUsers}")

  def chatPanic(mod: Holder, v: Boolean): Funit =
    slack(
      SlackMessage(
        username = mod.user.username,
        icon = if (v) "anger" else "information_source",
        text =
          s"${if (v) "Enabled" else "Disabled"} ${slackdown.lichessLink("mod/chat-panic", " Chat Panic")}",
        channel = SlackClient.rooms.tavern
      )
    ) >> zulip.mod()(
      s":stop: ${if (v) "Enabled" else "Disabled"} ${markdown.lichessLink("mod/chat-panic", " Chat Panic")}"
    )

  def garbageCollector(msg: String): Funit =
    slack(
      SlackMessage(
        username = "Garbage Collector",
        icon = "put_litter_in_its_place",
        text = slackdown.linkifyUsers(msg),
        channel = SlackClient.rooms.tavernBots
      )
    ) >> zulip.mod(ZulipClient.topic.altLog)(s":put_litter_in_its_place: ${markdown linkifyUsers msg}")

  def broadcastError(id: String, name: String, error: String): Funit =
    slack(
      SlackMessage(
        username = "lichess error",
        icon = "lightning",
        text = s"${slackdown.broadcastLink(id, name)}: $error",
        channel = SlackClient.rooms.broadcast
      )
    ) >> zulip(ZulipClient.stream.broadcast)(s":lightning: ${markdown.broadcastLink(id, name)}: $error")

  def userAppeal(user: User, mod: Holder): Funit =
    slack(
      SlackMessage(
        username = mod.user.username,
        icon = "eyes",
        text =
          s"Let's have a look at the appeal of _*${slackdown.lichessLink(s"/appeal/${user.username}", user.username)}*_",
        channel = SlackClient.rooms.tavernAppeal
      )
    ) >> zulip.mod(ZulipClient.topic.appeal)(
      s"Let's have a look at the appeal of _*${markdown.lichessLink(s"/appeal/${user.username}", user.username)}*_"
    )

  def stop(): Funit =
    slack(
      SlackMessage(
        username = "deployment",
        icon = "horsey",
        text = "Lichess is being updated! Brace for impact."
      )
    ) >> zulip()("Lichess is restarting.")

  def publishEvent(event: Event): Funit = event match {
    case Error(msg)   => publishError(msg)
    case Warning(msg) => publishWarning(msg)
    case Info(msg)    => publishInfo(msg)
    case Victory(msg) => publishVictory(msg)
  }

  private def publishError(msg: String): Funit =
    slack(
      SlackMessage(
        username = "lichess error",
        icon = "lightning",
        text = slackdown.linkifyUsers(msg)
      )
    ) >> zulip()(s":lightning: ${markdown linkifyUsers msg}")

  private def publishWarning(msg: String): Funit =
    slack(
      SlackMessage(
        username = "lichess warning",
        icon = "thinking_face",
        text = slackdown.linkifyUsers(msg)
      )
    ) >> zulip()(s":thinking: ${markdown linkifyUsers msg}")

  private def publishVictory(msg: String): Funit =
    slack(
      SlackMessage(
        username = "lichess victory",
        icon = "tada",
        text = slackdown.linkifyUsers(msg)
      )
    ) >> zulip()(s":tada: ${markdown linkifyUsers msg}")

  private[irc] def publishInfo(msg: String): Funit =
    slack(
      SlackMessage(
        username = "lichess info",
        icon = "horsey",
        text = slackdown linkifyUsers msg
      )
    ) >> zulip()(s":info: ${markdown linkifyUsers msg}")

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
      slack(
        SlackMessage(
          username = "Patron",
          icon = "four_leaf_clover",
          text = slackdown.linkifyUsers(text),
          channel = "team"
        )
      ) >> zulip()(markdown.linkifyUsers(text))

    private def userAt(username: String) =
      if (username == "Anonymous") "Anonymous"
      else s"@$username"

    private def amount(cents: Int) = s"$$${BigDecimal(cents.toLong, 2)}"
  }
}

object IrcApi {

  sealed trait MonitorType
  object MonitorType {
    case object Hunt  extends MonitorType
    case object Comm  extends MonitorType
    case object Other extends MonitorType
  }

  private val userRegex = lila.common.String.atUsernameRegex.pattern

  private object markdown { // both discord and zulip
    def link(url: String, name: String)         = s"[$name]($url)"
    def lichessLink(path: String, name: String) = s"[$name](https://lichess.org$path)"
    def userLink(name: String): String          = lichessLink(s"/@/$name?mod", name)
    def userLink(user: User): String            = userLink(user.username)
    def gameLink(id: String)                    = lichessLink(s"/$id", s"#$id")
    def userNotesLink(name: String)             = lichessLink(s"/@/$name?notes", "notes")
    def broadcastLink(id: String, name: String) = lichessLink(s"/broadcast/-/$id", name)
    val userReplace                             = link("https://lichess.org/@/$1?mod", "$1")
    def linkifyUsers(msg: String)               = userRegex matcher msg replaceAll userReplace
  }

  private object slackdown { // special markup for slack
    def link(url: String, name: String)         = s"<$url|$name>"
    def lichessLink(path: String, name: String) = s"<https://lichess.org$path|$name>"
    def userLink(name: String): String          = lichessLink(s"/@/$name?mod", name)
    def userLink(user: User): String            = userLink(user.username)
    def gameLink(id: String)                    = lichessLink(s"/$id", s"#$id")
    def userNotesLink(name: String)             = lichessLink(s"/@/$name?notes", "notes")
    def broadcastLink(id: String, name: String) = lichessLink(s"/broadcast/-/$id", name)
    val chatPanicLink                           = lichessLink("mod/chat-panic", "Chat Panic")
    val userReplace                             = link("https://lichess.org/@/$1?mod", "$1")
    def linkifyUsers(msg: String)               = userRegex matcher msg replaceAll userReplace
  }
}
