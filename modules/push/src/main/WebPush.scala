package lila.push

import com.softwaremill.tagging.*
import org.apache.pekko.stream.Materializer
import play.api.ConfigLoader
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.StandaloneWSClient
import scalalib.data.LazyFu

import lila.common.Json.given
import lila.common.autoconfig.*
import lila.mon.extensions.*

final private class BrowserWebPush(
    webSub: WebSubscriptionApi @@ BrowserSub,
    config: WebPush.Config,
    ws: StandaloneWSClient
)(using Executor, Materializer)
    extends WebPush(webSub, config, ws):

  protected def makeWebPayload(data: PushApi.Data) =
    Json.obj(
      "title" -> data.title,
      "body" -> data.body,
      "tag" -> data.key,
      "payload" -> Json
        .obj("userData" -> data.payload.userData.toMap)
        .add("userId" -> data.payload.userId)
    )

final private class UnifiedWebPush(
    webSub: WebSubscriptionApi @@ UnifiedSub,
    config: WebPush.Config,
    ws: StandaloneWSClient
)(using Executor, Materializer)
    extends WebPush(webSub, config, ws):

  protected def makeWebPayload(data: PushApi.Data) = FirebasePush.makeMobilePayload(data)

private abstract class WebPush(
    browserSub: WebSubscriptionApi,
    config: WebPush.Config,
    ws: StandaloneWSClient
)(using Executor, Materializer):

  protected def makeWebPayload(data: PushApi.Data): JsObject

  def apply(userId: UserId, data: LazyFu[PushApi.Data]): Funit =
    browserSub
      .getSubscriptions(5)(userId)
      .flatMap: subs =>
        subs.toNel.so: subs =>
          data.value.flatMap(sendToGroup(subs, List(userId)))

  def apply(userIds: Iterable[UserId], data: LazyFu[PushApi.Data]): Funit =
    browserSub
      .getSubscriptions(userIds, 5)
      .mapAsync(1): (subs, group) =>
        subs.toNel.so: neSubs =>
          data.value.flatMap(sendToGroup(neSubs, group))
      .run()
      .void

  private def sendToGroup(subs: NonEmptyList[WebSubscription], to: Iterable[UserId])(
      data: PushApi.Data
  ): Funit =
    config.url.nonEmpty.so:
      ws.url(config.url)
        .withHttpHeaders("ContentType" -> "application/json")
        .post(
          Json.obj(
            "subs" -> JsArray(subs.map { sub =>
              Json.obj(
                "endpoint" -> sub.endpoint,
                "keys" -> Json.obj(
                  "p256dh" -> sub.p256dh,
                  "auth" -> sub.auth
                )
              )
            }.toList),
            "payload" -> makeWebPayload(data).toString,
            "topic" -> data.key,
            "urgency" -> data.urgency,
            "ttl" -> 43200
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
                browserSub
                  .unsubscribeByEndpoints(staleEndpoints, to)
                  .map: n =>
                    logger.info(s"[push] web: $n/${staleEndpoints.size} stale endpoints unsubscribed")
          case res => fufail(s"[push] web: ${res.status} ${res.body}")
        .monSuccess(lila.mon.push.web.post)

private object WebPush:

  final class Config(
      val url: String,
      @ConfigName("vapid_public_key") val vapidPublicKey: String
  )
  given ConfigLoader[Config] = AutoConfig.loader[Config]
