package lila.push

import lila.common.autoconfig.*
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.StandaloneWSClient

import play.api.ConfigLoader

final private class WebPush(
    webSubscriptionApi: WebSubscriptionApi,
    config: WebPush.Config,
    ws: StandaloneWSClient
)(using Executor):

  def apply(userId: UserId, data: => PushApi.Data): Funit =
    webSubscriptionApi.getSubscriptions(5)(userId) flatMap { subscriptions =>
      subscriptions.toNel so send(data)
    }

  def apply(userIds: Iterable[UserId], data: => PushApi.Data): Funit =
    webSubscriptionApi.getSubscriptions(userIds, 5) flatMap { subs =>
      subs.toNel so send(data)
    }

  private def send(data: => PushApi.Data)(subscriptions: NonEmptyList[WebSubscription]): Funit =
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
          "topic"   -> data.stacking.key,
          "urgency" -> data.urgency.key,
          "ttl"     -> 43200
        )
      ) flatMap {
      case res if res.status == 200 => funit
      case res                      => fufail(s"[push] web: ${res.status} ${res.body}")
    }

private object WebPush:

  final class Config(
      val url: String,
      @ConfigName("vapid_public_key") val vapidPublicKey: String
  )
  given ConfigLoader[Config] = AutoConfig.loader[Config]
