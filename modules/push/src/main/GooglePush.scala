package lila.push

import play.api.libs.json._
import play.api.libs.ws.{ WS, WSAuthScheme }
import play.api.Play.current

private final class GooglePush(
    getDevices: String => Fu[List[Device]],
    url: String,
    key: String) {

  def apply(userId: String, title: String, body: String, payload: JsObject): Funit =
    getDevices(userId) flatMap { devices =>
      // deal only with one device per user for now, the one registered last
      // next is to implement devices groups management (and purging old devices):
      // https://developers.google.com/cloud-messaging/notifications#managing_device_groups
      devices.sortWith(_.seenAt isAfter _.seenAt).headOption.fold {
        fufail(s"[push] no device found for this user"): Funit
      } { device =>
        WS.url(s"$url/gcm/send")
          .withHeaders(
            "Authorization" -> s"key=$key",
            "Accept" -> "application/json",
            "Content-type" -> "application/json")
          .post(Json.obj(
            "to" -> device.id,
            "priority" -> "normal",
            "notification" -> Json.obj(
              "title" -> title,
              "body" -> body
            ),
            "data" -> payload
          )).flatMap {
            case res if res.status == 200 => funit
            case res                      => fufail(s"[push] ${device.id} $payload ${res.status} ${res.body}")
          }
      }
    }

}
