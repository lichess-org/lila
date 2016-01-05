package lila.slack

import play.api.libs.json._
import play.api.libs.ws.{ WS, WSAuthScheme }
import play.api.Play.current

private final class SlackClient(url: String) {

  def apply(msg: SlackMessage): Funit =
    WS.url(url)
      .post(Json.obj(
        "username" -> msg.username,
        "text" -> msg.text,
        "icon_emoji" -> s":${msg.icon}:"))
      .flatMap {
        case res if res.status == 200 => funit
        case res                      => fufail(s"[slack] $url $msg ${res.status} ${res.body}")
      }
}
