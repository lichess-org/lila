package lila.slack

import org.joda.time.DateTime

import lila.common.LightUser
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
        s"${link(event.username)} donated ${amount(event.amount)}. Monthly progress: ${event.percent}%"
      }

    private def addToBuffer(event: ChargeEvent): Funit = {
      buffer = buffer :+ event
      (buffer.head.date isBefore DateTime.now.minusHours(6)) ?? {
        val patrons = buffer map (_.username) map link mkString ", "
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

    private def link(username: String) =
      if (username == "Anonymous") "Anonymous"
      else s"@$username"

    private def amount(cents: Int) = s"$$${BigDecimal(cents, 2)}"
  }

  def publishEvent(event: Event): Funit = event match {
    case Error(msg) => publishError(msg)
    case Warning(msg) => publishWarning(msg)
    case Info(msg) => publishInfo(msg)
    case Victory(msg) => publishVictory(msg)
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

  private val chatPanicLink = "<https://lichess.org/mod/chat-panic|Chat Panic>"

  def chatPanic(mod: User, v: Boolean): Funit = client(SlackMessage(
    username = mod.username,
    icon = if (v) "anger" else "information_source",
    text = s"${if (v) "Enabled" else "Disabled"} $chatPanicLink",
    channel = rooms.tavern
  ))

  def garbageCollector(message: String): Funit = client(SlackMessage(
    username = "lichess",
    icon = "put_litter_in_its_place",
    text = linkifyUsers(message),
    channel = rooms.tavernBots
  ))

  def broadcastError(id: String, name: String): Funit = client(SlackMessage(
    username = "lichess error",
    icon = "lightning",
    text = s"${broadcastLink(id, name)} is failing",
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
      channel = rooms.general
    ))

  private def userLink(name: String) = s"<https://lichess.org/@/$name?mod|$name>"
  private def userNotesLink(name: String) = s"<https://lichess.org/@/$name?notes|notes>"
  private def broadcastLink(id: String, name: String) = s"<https://lichess.org/broadcast/-/$id|$name>"

  val userRegex = lila.common.String.atUsernameRegex.pattern

  private def linkifyUsers(msg: String) =
    userRegex matcher msg replaceAll "<https://lichess.org/@/$1?mod|@$1>"

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
      channel = rooms.general
    ))
}

private object SlackApi {

  object rooms {
    val general = "team"
    val tavern = "tavern"
    val tavernBots = "tavern-bots"
    val broadcast = "broadcast"
  }

  object stage {
    val name = "stage.lichess.org"
    val icon = "volcano"
  }
}
