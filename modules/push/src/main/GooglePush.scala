package lila.push

import play.api.libs.json._
import play.api.libs.ws.{ WS, WSAuthScheme }
import play.api.Play.current

private final class GooglePush(
    getDevice: String => Fu[Option[Device]],
    url: String,
    key: String) {

  def apply(userId: String)(data: => PushApi.Data): Funit =
    getDevice(userId) flatMap {
      _ ?? { device =>
        WS.url(url)
          .withHeaders(
            "Authorization" -> s"key=$key",
            "Accept" -> "application/json",
            "Content-type" -> "application/json")
          .post(Json.obj(
            "to" -> device.id,
            "priority" -> "normal",
            "notification" -> Json.obj(
              "title" -> data.title,
              "body" -> data.body
            ),
            "data" -> data.payload
          )).flatMap {
            case res if res.status == 200 => funit
            case res                      => fufail(s"[push] ${device.id} $data ${res.status} ${res.body}")
          }
      }
    }
}
