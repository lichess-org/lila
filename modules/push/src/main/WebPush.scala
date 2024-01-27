package lila.push

import lila.common.autoconfig.*
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.StandaloneWSClient

import play.api.ConfigLoader
import lila.common.LazyFu
import lila.common.Json.given

final private class WebPush(
    webSubscriptionApi: WebSubscriptionApi,
    config: WebPush.Config,
    ws: StandaloneWSClient
)(using Executor):

  def apply(userId: UserId, data: LazyFu[PushApi.Data]): Funit =
    webSubscriptionApi.getSubscriptions(5)(userId) flatMap sendTo(data)

  def apply(userIds: Iterable[UserId], data: LazyFu[PushApi.Data]): Funit =
    webSubscriptionApi.getSubscriptions(userIds, 5) flatMap sendTo(data)

  private def sendTo(data: LazyFu[PushApi.Data])(subs: List[WebSubscription]): Funit =
    subs.toNel.so: subs =>
      data.value flatMap send(subs)

  private def send(subscriptions: NonEmptyList[WebSubscription])(data: PushApi.Data): Funit =
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
              "title" -> data.title,
              "body"  -> data.body,
              "tag"   -> data.stacking.key,
              "payload" -> Json
                .obj("userData" -> data.payload.userData.toMap)
                .add("userId" -> data.payload.userId)
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
