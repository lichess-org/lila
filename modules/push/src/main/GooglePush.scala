package lila.push

import play.api.libs.json._
import play.api.libs.ws.{ WS, WSAuthScheme }
import play.api.Play.current

// https://aerogear.org/docs/specs/aerogear-unifiedpush-rest/index.html#397083935
private final class GooglePush(
    getDevices: String => Fu[List[Device]],
    url: String,
    key: String) {

  def apply(userId: String, payload: JsObject): Funit =
    getDevices(userId) flatMap { devices =>
      WS.url(s"$url/gcm/send")
        .withHeaders(
          "Authorization" -> s"key=$key",
          "Accept" -> "application/json",
          "Content-type" -> "application/json")
        .post(Json.obj(
          "registration_ids" -> devices.map(_.id),
          "data" -> payload
        )).flatMap {
          case res if res.status == 200 => funit
          case res                      => fufail(s"[push] ${devices.map(_.id)} $payload ${res.status} ${res.body}")
        }
    }
}
