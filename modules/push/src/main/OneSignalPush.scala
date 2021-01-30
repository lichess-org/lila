package lila.push

import io.methvin.play.autoconfig._
import play.api.libs.json._
import play.api.libs.ws._
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.JsonBodyWritables._

final private class OneSignalPush(
    deviceApi: DeviceApi,
    ws: StandaloneWSClient,
    config: OneSignalPush.Config
)(implicit ec: scala.concurrent.ExecutionContext) {

  import config._

  def apply(userId: String, data: => PushApi.Data): Funit =
    deviceApi.findLastManyByUserId("onesignal", 3)(userId) flatMap {
      case Nil => funit
      case devices =>
        ws.url(config.url)
          .withHttpHeaders(
            "Authorization" -> s"key=${key.value}",
            "Accept"        -> "application/json",
            "Content-type"  -> "application/json"
          )
          .post(
            Json.obj(
              "app_id"                -> appId,
              "include_player_ids"    -> devices.map(_.deviceId),
              "headings"              -> Map("en" -> data.title),
              "contents"              -> Map("en" -> data.body),
              "data"                  -> data.payload,
              "android_group"         -> data.stacking.key,
              "android_group_message" -> Map("en" -> data.stacking.message),
              "collapse_id"           -> data.stacking.key,
              "ios_badgeType"         -> "Increase",
              "ios_badgeCount"        -> 1
            )
          )
          .flatMap {
            case res if res.status == 200 || res.status == 400 =>
              readErrors(res)
                .filterNot(_ contains "must have English language")
                .filterNot(_ contains "All included players are not subscribed") match {
                case Nil => funit
                case errors =>
                  fufail(s"[push] ${devices.map(_.deviceId)} $data ${res.status} ${errors mkString ","}")
              }
            case res =>
              fufail(s"[push] ${devices.map(_.deviceId)} $data ${lila.log.http(res.status, res.body)}")
          }
    }

  private def readErrors(res: StandaloneWSResponse): List[String] =
    ~(res.body[JsValue] \ "errors").asOpt[List[String]]
}

private object OneSignalPush {

  final class Config(
      val url: String,
      @ConfigName("app_id") val appId: String,
      val key: lila.common.config.Secret
  )
  implicit val configLoader = AutoConfig.loader[Config]
}
