package lila.slack

import lila.common.LightUser
import lila.hub.actorApi.slack._
import lila.user.User

final class SlackApi(
    client: SlackClient,
    isProd: Boolean,
    implicit val lightUser: String => Option[LightUser]) {

  import SlackApi._

  def charge(event: lila.hub.actorApi.plan.ChargeEvent): Funit = {
    val amount = s"$$${lila.common.Maths.truncateAt(event.amount / 100d, 2)}"
    val link = s"lichess.org/@/${event.username}"
    client(SlackMessage(
      username = "Patron",
      icon = "four_leaf_clover",
      text = s"$link donated $amount. Monthly progress: ${event.percent}%",
      channel = "general"
    ))
  }

  def publishEvent(event: Event): Funit = event match {
    case Error(msg)   => publishError(msg)
    case Warning(msg) => publishWarning(msg)
    case Info(msg)    => publishInfo(msg)
    case Victory(msg) => publishVictory(msg)
  }

  def publishError(msg: String): Funit = client(SlackMessage(
    username = "lichess error",
    icon = "lightning",
    text = msg,
    channel = "general"))

  def publishWarning(msg: String): Funit = client(SlackMessage(
    username = "lichess warning",
    icon = "thinking_face",
    text = msg,
    channel = "general"))

  def publishVictory(msg: String): Funit = client(SlackMessage(
    username = "lichess victory",
    icon = "tada",
    text = msg,
    channel = "general"))

  def publishInfo(msg: String): Funit = client(SlackMessage(
    username = "lichess info",
    icon = "monkey",
    text = msg,
    channel = "general"))

  def publishRestart =
    if (isProd) publishInfo("Lichess has restarted!")
    else client(SlackMessage(
      username = stage.name,
      icon = stage.icon,
      text = "stage has restarted.",
      channel = "general"))

  def userMod(user: User, mod: User): Funit = client(SlackMessage(
    username = mod.username,
    icon = "oncoming_police_car",
    text = s"Let's have a look at https://lichess.org/@/${user.username}?mod",
    channel = "tavern"))

  def deployPre: Funit =
    if (isProd) client(SlackMessage(
      username = "deployment",
      icon = "rocket",
      text = "Lichess will be updated in a minute! Fasten your seatbelts.",
      channel = "general"))
    else client(SlackMessage(
      username = stage.name,
      icon = stage.icon,
      text = "stage will be updated in a minute.",
      channel = "general"))

  def deployPost: Funit =
    if (isProd) client(SlackMessage(
      username = "deployment",
      icon = "rocket",
      text = "Lichess is being updated! Brace for impact.",
      channel = "general"))
    else client(SlackMessage(
      username = "stage.lichess.org",
      icon = "volcano",
      text = "stage has been updated!",
      channel = "general"))
}

private object SlackApi {

  object stage {
    val name = "stage.lichess.org"
    val icon = "volcano"
  }
}
