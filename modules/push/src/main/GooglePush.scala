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
            "to" -> device.deviceId,
            "priority" -> "normal",
            "notification" -> Json.obj(
              "title" -> data.title,
              "body" -> data.body
            ),
            "data" -> data.payload.++(Json.obj(
              // https://github.com/phonegap/phonegap-plugin-push/blob/master/docs/PAYLOAD.md#sound
              "soundname" -> "default",
              // https://github.com/phonegap/phonegap-plugin-push/blob/master/docs/PAYLOAD.md#led-in-notifications
              "ledColor" -> List(0, 56, 147, 232), // startup blue
              "vibrationPattern" -> List(1000, 1000) // wait 1s, vibrate 1s only once
              ))
          )).flatMap {
            case res if res.status == 200 => funit
            case res                      => fufail(s"[push] ${device.deviceId} $data ${res.status} ${res.body}")
          }
      }
    }
}
