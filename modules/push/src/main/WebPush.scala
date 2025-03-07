package lila.push

import play.api.ConfigLoader
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.StandaloneWSClient
import scalalib.data.LazyFu

import lila.common.Json.given
import lila.common.autoconfig.*

final private class WebPush(
    webSubscriptionApi: WebSubscriptionApi,
    config: WebPush.Config,
    ws: StandaloneWSClient
)(using Executor):

  def apply(userId: UserId, data: LazyFu[PushApi.Data]): Funit =
    webSubscriptionApi.getSubscriptions(5)(userId).flatMap(sendTo(data, List(userId)))

  def apply(userIds: Iterable[UserId], data: LazyFu[PushApi.Data]): Funit =
    webSubscriptionApi.getSubscriptions(userIds, 5).flatMap(sendTo(data, userIds))

  private def sendTo(data: LazyFu[PushApi.Data], to: Iterable[UserId])(subs: List[WebSubscription]): Funit =
    subs.toNel.so: subs =>
      data.value.flatMap(send(subs, to))

  private def send(subscriptions: NonEmptyList[WebSubscription], to: Iterable[UserId])(
      data: PushApi.Data
  ): Funit =
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
      )
      .flatMap:
        case res if res.status == 200 =>
          res
            .body[JsValue]
            .asOpt[JsObject]
            .map:
              _.fields.collect:
                case (endpoint, JsString("endpoint_not_valid" | "endpoint_not_found")) => endpoint
            .filter(_.nonEmpty)
            .so: staleEndpoints =>
              webSubscriptionApi
                .unsubscribeByEndpoints(staleEndpoints, to)
                .map: n =>
                  logger.info(s"[push] web: $n/${staleEndpoints.size} stale endpoints unsubscribed")
        case res => fufail(s"[push] web: ${res.status} ${res.body}")

private object WebPush:

  final class Config(
      val url: String,
      @ConfigName("vapid_public_key") val vapidPublicKey: String
  )
  given ConfigLoader[Config] = AutoConfig.loader[Config]
