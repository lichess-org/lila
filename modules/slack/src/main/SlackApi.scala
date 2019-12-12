package lila.slack

import org.joda.time.DateTime

import lila.common.{ LightUser, IpAddress, EmailAddress }
import lila.hub.actorApi.slack._
import lila.user.User

final class SlackApi(
    client: SlackClient,
    isProd: Boolean,
    implicit val lightUser: LightUser.Getter
) {

  import SlackApi._

  object charge {

    import lila.hub.actorApi.plan.ChargeEvent

    private var buffer: Vector[ChargeEvent] = Vector.empty

    def apply(event: ChargeEvent): Funit =
      if (event.amount < 5000) addToBuffer(event)
      else displayMessage {
        s"${userAt(event.username)} donated ${amount(event.amount)}. Monthly progress: ${event.percent}%"
      }

    private def addToBuffer(event: ChargeEvent): Funit = {
      buffer = buffer :+ event
      (buffer.head.date isBefore DateTime.now.minusHours(6)) ?? {
        val patrons = buffer map (_.username) map userAt mkString ", "
        val amountSum = buffer.map(_.amount).sum
        displayMessage {
          s"$patrons donated ${amount(amountSum)}. Monthly progress: ${buffer.last.percent}%"
        } >>- {
          buffer = Vector.empty
        }
      }
    }

    private def displayMessage(text: String) = client(SlackMessage(
      username = "Patron",
      icon = "four_leaf_clover",
      text = linkifyUsers(text),
      channel = "team"
    ))

    private def userAt(username: String) =
      if (username == "Anonymous") "Anonymous"
      else s"@$username"

    private def amount(cents: Int) = s"$$${BigDecimal(cents, 2)}"
  }

  def publishEvent(event: Event): Funit = event match {
    case Error(msg) => publishError(msg)
    case Warning(msg) => publishWarning(msg)
    case Info(msg) => publishInfo(msg)
    case Victory(msg) => publishVictory(msg)
    case TournamentName(userName, tourId, tourName) => client(SlackMessage(
      username = "Tournament name alert",
      icon = "children_crossing",
      text = s"${userLink(userName)} created ${link(s"https://lichess.org/tournament/$tourId", s"$tourName Arena")}",
      channel = rooms.tavern
    ))
  }

  def commlog(mod: User, user: User, reportBy: Option[User.ID]): Funit = client(SlackMessage(
    username = mod.username,
    icon = "eye",
    text = {
      val finalS = if (user.username endsWith "s") "" else "s"
      s"checked out _*${userLink(user.username)}*_'$finalS communications "
    } + reportBy.filter(mod.id !=).fold("spontaneously") { by =>
      s"while investigating a report created by ${userLink(by)}"
    },
    channel = "commlog"
  ))

  def chatPanic(mod: User, v: Boolean): Funit = client(SlackMessage(
    username = mod.username,
    icon = if (v) "anger" else "information_source",
    text = s"${if (v) "Enabled" else "Disabled"} $chatPanicLink",
    channel = rooms.tavern
  ))

  def garbageCollector(message: String): Funit = client(SlackMessage(
    username = "Garbage Collector",
    icon = "put_litter_in_its_place",
    text = linkifyUsers(message),
    channel = rooms.tavernBots
  ))

  def selfReport(typ: String, path: String, user: User, ip: IpAddress): Funit = client(SlackMessage(
    username = "Self Report",
    icon = "kms",
    text = s"[*$typ*] ${userLink(user)}@$ip ${gameLink(path)}",
    channel = rooms.tavernBots
  ))

  def commReportBurst(user: User): Funit = client(SlackMessage(
    username = "Comm alert",
    icon = "anger",
    text = linkifyUsers(s"Burst of comm reports about @${user.username}"),
    channel = rooms.tavern
  ))

  def broadcastError(id: String, name: String, error: String): Funit = client(SlackMessage(
    username = "lichess error",
    icon = "lightning",
    text = s"${broadcastLink(id, name)}: $error",
    channel = rooms.broadcast
  ))

  def publishError(msg: String): Funit = client(SlackMessage(
    username = "lichess error",
    icon = "lightning",
    text = linkifyUsers(msg),
    channel = rooms.general
  ))

  def publishWarning(msg: String): Funit = client(SlackMessage(
    username = "lichess warning",
    icon = "thinking_face",
    text = linkifyUsers(msg),
    channel = rooms.general
  ))

  def publishVictory(msg: String): Funit = client(SlackMessage(
    username = "lichess victory",
    icon = "tada",
    text = linkifyUsers(msg),
    channel = rooms.general
  ))

  def publishInfo(msg: String): Funit = client(SlackMessage(
    username = "lichess info",
    icon = "monkey",
    text = linkifyUsers(msg),
    channel = rooms.general
  ))

  def publishRestart =
    if (isProd) publishInfo("Lichess has restarted!")
    else client(SlackMessage(
      username = stage.name,
      icon = stage.icon,
      text = "stage has restarted.",
      channel = rooms.devNoise
    ))

  private def link(url: String, name: String) = s"<$url|$name>"
  private def lichessLink(path: String, name: String) = s"<https://lichess.org$path|$name>"
  private def userLink(name: String): String = lichessLink(s"/@/$name?mod", name)
  private def userLink(user: User): String = userLink(user.username)
  private def gameLink(id: String) = lichessLink(s"/$id", s"#$id")
  private def userNotesLink(name: String) = lichessLink(s"/@/$name?notes", "notes")
  private def broadcastLink(id: String, name: String) = lichessLink(s"/broadcast/-/$id", name)
  private val chatPanicLink = lichessLink("mod/chat-panic", "Chat Panic")

  private val userRegex = lila.common.String.atUsernameRegex.pattern
  private val userReplace = link("https://lichess.org/@/$1?mod", "$1")

  private def linkifyUsers(msg: String) =
    userRegex matcher msg replaceAll userReplace

  def userMod(user: User, mod: User): Funit = client(SlackMessage(
    username = mod.username,
    icon = "eyes",
    text = s"Let's have a look at _*${userLink(user.username)}*_",
    channel = rooms.tavern
  ))

  def userModNote(modName: String, username: String, note: String): Funit =
    client(SlackMessage(
      username = modName,
      icon = "spiral_note_pad",
      text = (s"_*${userLink(username)}*_ (${userNotesLink(username)}):\n" +
        linkifyUsers(note take 2000)),
      channel = rooms.tavern
    ))

  def deployPre: Funit =
    if (isProd) client(SlackMessage(
      username = "deployment",
      icon = "rocket",
      text = "Lichess will be updated in a minute! Fasten your seatbelts.",
      channel = rooms.general
    ))
    else client(SlackMessage(
      username = stage.name,
      icon = stage.icon,
      text = "stage will be updated in a minute.",
      channel = rooms.general
    ))

  def deployPost: Funit =
    if (isProd) client(SlackMessage(
      username = "deployment",
      icon = "rocket",
      text = "Lichess is being updated! Brace for impact.",
      channel = rooms.general
    ))
    else client(SlackMessage(
      username = "stage.lichess.org",
      icon = "volcano",
      text = "stage has been updated!",
      channel = rooms.devNoise
    ))

  def signup(user: User, email: EmailAddress, ip: IpAddress, fp: Option[String], susp: Boolean) = client(SlackMessage(
    username = "lichess",
    icon = "musical_note",
    text = {
      val emailLink = lichessLink(s"/mod/search?q=${email.value}", email.value)
      val ipLink = lichessLink(s"/mod/search?q=$ip", ip.value)
      val fpLink = fp.fold("none")(print => lichessLink(s"/mod/print/$print", print))
      s"${userLink(user.username)} EMAIL: $emailLink IP: $ipLink FP: $fpLink${susp ?? " *proxy*"}"
    },
    channel = rooms.signups
  ))
}

private object SlackApi {

  object rooms {
    val general = "team"
    val tavern = "tavern"
    val tavernBots = "tavern-bots"
    val signups = "signups"
    val broadcast = "broadcast"
    val devNoise = "dev-noise"
  }

  object stage {
    val name = "stage.lichess.org"
    val icon = "volcano"
  }
}
