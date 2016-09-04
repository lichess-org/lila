package lila.push

import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.Play.current

private final class OneSignalPush(
    getDevice: String => Fu[Option[Device]],
    url: String,
    appId: String,
    key: String) {

  def apply(userId: String)(data: => PushApi.Data): Funit =
    getDevice(userId.pp("find device")).thenPp("device found") flatMap {
      _ ?? { device =>
        WS.url(url)
          .withHeaders(
            "Authorization" -> s"key=$key",
            "Accept" -> "application/json",
            "Content-type" -> "application/json")
          .post(Json.obj(
            "app_id" -> appId,
            "include_player_ids" -> List(device.deviceId),
            "headings" -> Map("en" -> data.title),
            "contents" -> Map("en" -> data.body),
            "data" -> data.payload
          ).pp).flatMap {
            case res if res.status == 200 => funit
            case res                      => fufail(s"[push] ${device.deviceId} $data ${res.status} ${res.body}")
          }.thenPp
      }
    }
}
