package lila.push

import io.methvin.play.autoconfig._
import play.api.libs.json._
import play.api.libs.ws.WSClient
import cats.data.NonEmptyList

import lila.user.User

final private class WebPush(
    webSubscriptionApi: WebSubscriptionApi,
    config: WebPush.Config,
    ws: WSClient
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(userId: User.ID, data: => PushApi.Data): Funit =
    webSubscriptionApi.getSubscriptions(5)(userId) flatMap { subscriptions =>
      subscriptions.toNel ?? send(data)
    }

  private def send(data: => PushApi.Data)(subscriptions: NonEmptyList[WebSubscription]): Funit =
    if (config.url.isEmpty) funit
    else
      ws.url(config.url)
        .withHttpHeaders("ContentType" -> "application/json")
        .post(
          Json.obj(
            "subs" -> JsArray(subscriptions.map { sub =>
              Json.obj(
                "endpoint" -> sub.endpoint,
                "keys" -> Json.obj(
                  "p256dh" -> sub.p256dh,
                  "auth"   -> sub.auth
                )
              )
            }.toList),
            "payload" -> Json
              .obj(
                "title"   -> data.title,
                "body"    -> data.body,
                "tag"     -> data.stacking.key,
                "payload" -> data.payload
              )
              .toString,
            "ttl" -> 43200
          )
        ) flatMap {
        case res if res.status == 200 => funit
        case res                      => fufail(s"[push] web: ${res.status} ${res.body}")
      }
}

private object WebPush {

  final class Config(
      val url: String,
      @ConfigName("vapid_public_key") val vapidPublicKey: String
  )
  implicit val configLoader = AutoConfig.loader[Config]
}
