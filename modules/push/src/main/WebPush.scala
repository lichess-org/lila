package lila.push

import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.Play.current

import lila.user.User

private final class WebPush(
    getSubscriptions: User.ID => Fu[List[WebSubscription]],
    url: String,
    vapidPublicKey: String
) {

  def apply(userId: User.ID)(data: => PushApi.Data): Funit =
    getSubscriptions(userId) flatMap { subscriptions: List[WebSubscription] =>
      WS.url(url)
        .withHeaders("ContentType" -> "application/json")
        .post(Json.obj(
          "subs" -> JsArray(subscriptions.map { sub =>
            Json.obj(
              "endpoint" -> sub.endpoint,
              "keys" -> Json.obj(
                "p256dh" -> sub.p256dh,
                "auth" -> sub.auth
              )
            )
          }),
          "payload" -> Json.obj(
            "title" -> data.title,
            "body" -> data.body,
            "stacking" -> data.stacking.key,
            "payload" -> data.payload
          ).toString,
          "ttl" -> 43200
        ))
    } flatMap {
      case res if res.status == 200 => funit
      case res => fufail(s"[push] web: ${res.status} ${res.body}")
    }
}
