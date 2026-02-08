package lila.push

import com.google.auth.oauth2.{ AccessToken, GoogleCredentials, ServiceAccountCredentials }
import play.api.ConfigLoader
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.StandaloneWSClient
import scalalib.cache.FrequencyThreshold
import scalalib.data.LazyFu

import lila.common.Chronometer

final private class FirebasePush(
    deviceApi: DeviceApi,
    ws: StandaloneWSClient,
    configs: FirebasePush.BothConfigs
)(using Executor, Scheduler):

  if configs.lichobile.googleCredentials.isDefined then
    logger.info("Lichobile Firebase push notifications are enabled.")
  if configs.mobile.googleCredentials.isDefined then
    logger.info("Mobile Firebase push notifications are enabled.")

  private val workQueue =
    scalalib.actor.AsyncActorSequencer(
      maxSize = Max(512),
      timeout = 10.seconds,
      name = "firebasePush",
      lila.log.asyncActorMonitor.full
    )

  def apply(userId: UserId, data: LazyFu[PushApi.Data]): Funit =
    deviceApi
      .findLastManyByUserId("firebase", 3)(userId)
      .flatMap:
        _.sequentiallyVoid: device =>
          val config = if device.isMobile then configs.mobile else configs.lichobile
          config.googleCredentials.so: creds =>
            for
              data <- data.value
              _ <-
                if !data.mobileCompatible.exists(device.isMobileVersionCompatible)
                then funit // don't send to mobile if incompatible version
                else if data.firebaseMod.contains(PushApi.Data.FirebaseMod.DataOnly) && !device.isMobile
                then funit // don't send data messages to lichobile
                else
                  for
                    // access token has 1h lifetime and is requested only if expired
                    token <- workQueue {
                      Future:
                        Chronometer.syncMon(_.blocking.time("firebase")):
                          creds.refreshIfExpired()
                          creds.getAccessToken()
                    }.chronometer.mon(_.push.googleTokenTime).result
                    _ <- send(token, device, config, data)
                  yield ()
            yield ()

  opaque type StatusCode = Int
  object StatusCode extends OpaqueInt[StatusCode]
  private val errorCounter = FrequencyThreshold[StatusCode](50, 10.minutes)

  private def send(
      token: AccessToken,
      device: Device,
      config: FirebasePush.Config,
      data: PushApi.Data
  ): Funit =
    ws.url(config.url)
      .withHttpHeaders(
        "Authorization" -> s"Bearer ${token.getTokenValue}",
        "Accept" -> "application/json",
        "Content-type" -> "application/json; UTF-8"
      )
      .post:
        Json.obj(
          "message" -> Json
            .obj(
              "token" -> device._id,
              "data" -> toDataKeyValue:
                data.firebaseMod.match
                  case Some(PushApi.Data.FirebaseMod.NotifOnly(mod)) => mod(data.payload.userData)
                  case _ =>
                    data.payload.userData ++ (data.iosBadge.map: number =>
                      "iosBadge" -> number.toString),
              "android" -> Json.obj("priority" -> "high")
            )
            .add:
              "notification" -> data.firebaseMod.match
                case Some(PushApi.Data.FirebaseMod.DataOnly) => none
                case _ => Json.obj("body" -> data.body, "title" -> data.title).some
            .add:
              "apns" -> data.iosBadge.map: number =>
                Json.obj(
                  "headers" -> Json.obj("apns-priority" -> "10"),
                  "payload" -> Json.obj:
                    "aps" -> Json.obj("badge" -> number)
                )
        )
      .flatMap: res =>
        lila.mon.push.firebaseStatus(res.status).increment()
        lila.mon.push
          .firebaseType(data.firebaseMod.fold("both"):
            case PushApi.Data.FirebaseMod.DataOnly => "data"
            case PushApi.Data.FirebaseMod.NotifOnly(_) => "notif")
          .increment()
        if res.status == 200 then funit
        else if res.status == 404 then
          logger.info(s"Delete missing firebase device $device")
          deviceApi.delete(device)
        else
          if errorCounter(res.status) then logger.warn(s"[push] firebase: ${res.status}")
          funit

  private def toDataKeyValue(data: PushApi.Data.KeyValue): JsObject = JsObject:
    data.view
      .map: (k, v) =>
        s"lichess.$k" -> JsString(v)
      .toMap

private object FirebasePush:

  final class Config(val url: String, val json: lila.core.config.Secret):
    lazy val googleCredentials: Option[GoogleCredentials] =
      try
        json.value.nonEmptyOption.map: json =>
          import java.nio.charset.StandardCharsets.UTF_8
          import scala.jdk.CollectionConverters.*
          ServiceAccountCredentials
            .fromStream(new java.io.ByteArrayInputStream(json.getBytes(UTF_8)))
            .createScoped(Set("https://www.googleapis.com/auth/firebase.messaging").asJava)
      catch
        case e: Exception =>
          logger.warn("Failed to create google credentials", e)
          none
  final class BothConfigs(val lichobile: Config, val mobile: Config)
  import lila.common.autoconfig.*
  import lila.common.config.given
  given ConfigLoader[Config] = AutoConfig.loader[Config]
  given ConfigLoader[BothConfigs] = AutoConfig.loader[BothConfigs]
