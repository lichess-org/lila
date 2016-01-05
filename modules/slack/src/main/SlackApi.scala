package lila.slack

import lila.common.LightUser

private final class SlackApi(
    client: SlackClient,
    implicit val lightUser: String => Option[LightUser]) {

  def donation(event: lila.hub.actorApi.DonationEvent): Funit = {
    val user = event.userId flatMap lightUser
    val username = user.fold("Anonymous")(_.titleName)
    def amount(cents: Int) = s"$$${lila.common.Maths.truncateAt(cents / 100d, 2)}"
    client(SlackMessage(
      username = "donation",
      icon = "heart_eyes",
      text = s"$username donated ${amount(event.gross)} (${amount(event.net)})! Monthly progress: ${event.progress}%"
    )) >> event.message.?? { msg =>
    client(SlackMessage(
      username = username,
      icon = "kissing_heart",
      text = msg))
    }
  }
}
