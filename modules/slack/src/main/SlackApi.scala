package lidraughts.slack

import org.joda.time.DateTime

import lidraughts.common.LightUser
import lidraughts.hub.actorApi.slack._
import lidraughts.user.User

final class SlackApi(
    client: SlackClient,
    isProd: Boolean,
    implicit val lightUser: LightUser.Getter
) {

  import SlackApi._

  object charge {

    import lidraughts.hub.actorApi.plan.ChargeEvent

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
      channel = "general"
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

  def chatPanicLink = "<https://lidraughts.org/mod/chat-panic|Chat Panic>"

  def chatPanic(mod: User, v: Boolean): Funit = client(SlackMessage(
    username = mod.username,
    icon = if (v) "anger" else "information_source",
    text = s"${if (v) "Enabled" else "Disabled"} $chatPanicLink",
    channel = "tavern"
  ))

  def garbageCollector(message: String): Funit = client(SlackMessage(
    username = "lidraughts",
    icon = "put_litter_in_its_place",
    text = linkifyUsers(message),
    channel = "tavern"
  ))

  def publishError(msg: String): Funit = client(SlackMessage(
    username = "lidraughts error",
    icon = "lightning",
    text = linkifyUsers(msg),
    channel = "general"
  ))

  def publishWarning(msg: String): Funit = client(SlackMessage(
    username = "lidraughts warning",
    icon = "thinking_face",
    text = linkifyUsers(msg),
    channel = "general"
  ))

  def publishVictory(msg: String): Funit = client(SlackMessage(
    username = "lidraughts victory",
    icon = "tada",
    text = linkifyUsers(msg),
    channel = "general"
  ))

  def publishInfo(msg: String): Funit = client(SlackMessage(
    username = "lidraughts info",
    icon = "monkey",
    text = linkifyUsers(msg),
    channel = "general"
  ))

  def publishRestart =
    if (isProd) publishInfo("Lidraughts has restarted!")
    else client(SlackMessage(
      username = stage.name,
      icon = stage.icon,
      text = "stage has restarted.",
      channel = "general"
    ))

  private def userLink(name: String) = s"<https://lidraughts.org/@/$name?mod|$name>"
  private def userNotesLink(name: String) = s"<https://lidraughts.org/@/$name?notes|notes>"

  val userRegex = lidraughts.common.String.atUsernameRegex.pattern

  private def linkifyUsers(msg: String) =
    userRegex matcher msg replaceAll "<https://lidraughts.org/@/$1?mod|@$1>"

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
      text = "Lidraughts will be updated in a minute! Fasten your seatbelts.",
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
      text = "Lidraughts is being updated! Brace for impact.",
      channel = "general"
    ))
    else client(SlackMessage(
      username = "stage.lidraughts.org",
      icon = "volcano",
      text = "stage has been updated!",
      channel = "general"
    ))
}

private object SlackApi {

  object stage {
    val name = "stage.lidraughts.org"
    val icon = "volcano"
  }
}
