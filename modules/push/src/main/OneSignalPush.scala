package lila.push

import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.Play.current

private final class OneSignalPush(
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
            "include_player_ids" -> List[String](),
            // "include_player_ids" -> List(device.deviceId),
            "headings" -> Map("en" -> data.title),
            "contents" -> Map("en" -> data.body),
            "data" -> data.payload
          )).flatMap {
            case res if res.status == 200 => funit
            case res                      => fufail(s"[push] ${device.deviceId} $data ${res.status} ${res.body}")
          }
      }
    }
}

