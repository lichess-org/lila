package lila.slack

import scala.concurrent.duration._

import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.Play.current

import lila.memo.RateLimit

private final class SlackClient(url: String, defaultChannel: String) {

  private val limiter = new RateLimit[SlackMessage](
    credits = 1,
    duration = 15 minutes,
    name = "slack client",
    key = "slack.client"
  )

  def apply(msg: SlackMessage): Funit = limiter(msg) {
    if (url.isEmpty) fuccess(lila.log("slack").info(msg.toString))
    else WS.url(url)
      .post(Json.obj(
        "username" -> msg.username,
        "text" -> msg.text,
        "icon_emoji" -> s":${msg.icon}:",
        "channel" -> (msg.channel != defaultChannel).option(s"#${msg.channel}")
      ).noNull).flatMap {
        case res if res.status == 200 => funit
        case res => fufail(s"[slack] $url $msg ${res.status} ${res.body}")
      }
  }
}
