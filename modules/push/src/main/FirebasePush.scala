package lila.push

import com.google.auth.oauth2.{ AccessToken, GoogleCredentials, ServiceAccountCredentials }
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.StandaloneWSClient
import scala.concurrent.blocking

import lila.common.{ Chronometer, LazyFu }
import lila.memo.FrequencyThreshold
import play.api.ConfigLoader
import lila.common.config.Max

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
    lila.hub.AsyncActorSequencer(maxSize = Max(512), timeout = 10 seconds, name = "firebasePush")

  def apply(userId: UserId, data: LazyFu[PushApi.Data]): Funit =
    deviceApi.findLastManyByUserId("firebase", 3)(userId) flatMap:
      _.traverse_ { device =>
        val config = if device.isMobile then configs.mobile else configs.lichobile
        config.googleCredentials.so: creds =>
          for
            // access token has 1h lifetime and is requested only if expired
            token <- workQueue {
              Future:
                Chronometer.syncMon(_.blocking time "firebase"):
                  blocking:
                    creds.refreshIfExpired()
                    creds.getAccessToken()
            }.chronometer.mon(_.push.googleTokenTime).result
            data <- data.value
            _    <- send(token, device, config, data)
          yield ()
      }

  opaque type StatusCode = Int
  object StatusCode extends OpaqueInt[StatusCode]
  private val errorCounter = FrequencyThreshold[StatusCode](50, 10 minutes)

  private def send(
      token: AccessToken,
      device: Device,
      config: FirebasePush.Config,
      data: PushApi.Data
  ): Funit =
    ws.url(config.url)
      .withHttpHeaders(
        "Authorization" -> s"Bearer ${token.getTokenValue}",
        "Accept"        -> "application/json",
        "Content-type"  -> "application/json; UTF-8"
      )
      .post:
        Json.obj(
          "message" -> Json
            .obj(
              "token" -> device._id,
              "data"  -> toDataKeyValue(data.payload),
              "notification" -> Json.obj(
                "body"  -> data.body,
                "title" -> data.title
              )
            )
            .add:
              "apns" -> data.iosBadge.map: number =>
                Json.obj:
                  "payload" -> Json.obj:
                    "aps" -> Json.obj("badge" -> number)
        )
      .flatMap: res =>
        lila.mon.push.firebaseStatus(res.status).increment()
        if res.status == 200 then funit
        else if res.status == 404 then
          logger.info(s"Delete missing firebase device $device")
          deviceApi delete device
        else
          if errorCounter(res.status) then logger.warn(s"[push] firebase: ${res.status}")
          funit

  // firebase doesn't support nested data object and we only use what is inside userData
  private def toDataKeyValue(data: PushApi.Data.Payload): JsObject = JsObject:
    data.userData.view
      .map: (k, v) =>
        s"lichess.$k" -> JsString(v)
      .toMap

private object FirebasePush:

  final class Config(val url: String, val json: lila.common.config.Secret):
    lazy val googleCredentials: Option[GoogleCredentials] =
      try
        json.value.some.filter(_.nonEmpty) map: json =>
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
  given ConfigLoader[Config]      = AutoConfig.loader[Config]
  given ConfigLoader[BothConfigs] = AutoConfig.loader[BothConfigs]
