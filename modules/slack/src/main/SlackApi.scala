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
        s"${link(event)} donated ${amount(event.amount)}. Monthly progress: ${event.percent}%"
      }

    private def addToBuffer(event: ChargeEvent): Funit = {
      buffer = buffer :+ event
      (buffer.head.date isBefore DateTime.now.minusHours(4)) ?? {
        val patrons = buffer map (_.username) mkString ", "
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
      text = text,
      channel = "general"
    ))

    private def link(event: ChargeEvent) =
      if (event.username == "Anonymous") "Anonymous"
      else s"lichess.org/@/${event.username}"

    private def amount(cents: Int) = s"$$${BigDecimal(cents, 2)}"
  }

  def publishEvent(event: Event): Funit = event match {
    case Error(msg) => publishError(msg)
    case Warning(msg) => publishWarning(msg)
    case Info(msg) => publishInfo(msg)
    case Victory(msg) => publishVictory(msg)
  }

  def publishError(msg: String): Funit = client(SlackMessage(
    username = "lichess error",
    icon = "lightning",
    text = linkifyUsers(msg),
    channel = "general"
  ))

  def publishWarning(msg: String): Funit = client(SlackMessage(
    username = "lichess warning",
    icon = "thinking_face",
    text = linkifyUsers(msg),
    channel = "general"
  ))

  def publishVictory(msg: String): Funit = client(SlackMessage(
    username = "lichess victory",
    icon = "tada",
    text = linkifyUsers(msg),
    channel = "general"
  ))

  def publishInfo(msg: String): Funit = client(SlackMessage(
    username = "lichess info",
    icon = "monkey",
    text = linkifyUsers(msg),
    channel = "general"
  ))

  def publishRestart =
    if (isProd) publishInfo("Lichess has restarted!")
    else client(SlackMessage(
      username = stage.name,
      icon = stage.icon,
      text = "stage has restarted.",
      channel = "general"
    ))

  private def userLink(name: String) = s"<https://lichess.org/@/$name?mod|$name>"
  private def userNotesLink(name: String) = s"<https://lichess.org/@/$name?notes|notes>"

  private val userRegex = """(^|\s)@(\w[-_\w]+)\b""".r.pattern
  private def linkifyUsers(msg: String) =
    userRegex matcher msg replaceAll "$1<https://lichess.org/@/$2?mod|@$2>"

  def userMod(user: User, mod: User): Funit = client(SlackMessage(
    username = mod.username,
    icon = "eyes",
    text = s"Let's have a look at _*${userLink(user.username)}*_",
    channel = "tavern"
  ))

  def userModNote(modName: String, username: String, note: String): Funit =
    client(SlackMessage(
      username = modName,
      icon = "spiral_note_pad",
      text = (s"_*${userLink(username)}*_ (${userNotesLink(username)}):\n" +
      linkifyUsers(note take 2000)),
      channel = "tavern"
    ))

  def deployPre: Funit =
    if (isProd) client(SlackMessage(
      username = "deployment",
      icon = "rocket",
      text = "Lichess will be updated in a minute! Fasten your seatbelts.",
      channel = "general"
    ))
    else client(SlackMessage(
      username = stage.name,
      icon = stage.icon,
      text = "stage will be updated in a minute.",
      channel = "general"
    ))

  def deployPost: Funit =
    if (isProd) client(SlackMessage(
      username = "deployment",
      icon = "rocket",
      text = "Lichess is being updated! Brace for impact.",
      channel = "general"
    ))
    else client(SlackMessage(
      username = "stage.lichess.org",
      icon = "volcano",
      text = "stage has been updated!",
      channel = "general"
    ))
}

private object SlackApi {

  object stage {
    val name = "stage.lichess.org"
    val icon = "volcano"
  }
}
