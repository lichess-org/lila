package lila.slack

import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.Play.current

import lila.common.PimpedJson._

private final class SlackClient(url: String, defaultChannel: String) {

  def apply(msg: SlackMessage): Funit =
    url.nonEmpty ?? WS.url(url)
      .post(Json.obj(
        "username" -> msg.username,
        "text" -> msg.text,
        "icon_emoji" -> s":${msg.icon}:",
        "channel" -> (msg.channel != defaultChannel).option(s"#${msg.channel}")
      ).noNull).flatMap {
        case res if res.status == 200 => funit
        case res                      => fufail(s"[slack] $url $msg ${res.status} ${res.body}")
      }
}
