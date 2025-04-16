package lila.ublog

import com.typesafe.config.{ Config, ConfigFactory }
import play.api.{ ConfigLoader, Configuration }
import play.api.libs.json.*
import play.api.libs.ws.*
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.DefaultBodyReadables.readableAsString

import lila.common.autoconfig.AutoConfig
import lila.memo.SettingStore
import lila.core.data.Text
import lila.memo.SettingStore.Text.given

case class UblogAutomodConfig(
    apikey: String,
    model: String,
    url: String
)

final class UblogAutomod(
    ws: StandaloneWSClient,
    appConfig: Configuration,
    settingStore: lila.memo.SettingStore.Builder
)(using Executor):

  val promptSetting = settingStore[Text](
    "ublogAutomodPrompt",
    text = "Ublog automod prompt".some,
    default = Text("")
  )

  private val cfg = appConfig.get[UblogAutomodConfig]("ublog.automod")(AutoConfig.loader)

  def fetch(userText: String): Fu[Option[JsObject]] =
    val prompt = promptSetting.get().value
    prompt.nonEmpty.so:
      val body = Json.obj(
        "model" -> cfg.model,
        // "response_format" -> "json", // not universally supported it seems
        // "temperature" -> 0.7,
        // "top_p" -> 1,
        // "frequency_penalty" -> 0,
        // "presence_penalty" -> 0
        "messages" -> JsArray(
          List(
            Json.obj("role" -> "system", "content" -> prompt),
            Json.obj("role" -> "user", "content"   -> userText)
          )
        )
      )
      ws.url(cfg.url)
        .withHttpHeaders(
          "Authorization" -> s"Bearer ${cfg.apikey}",
          "Content-Type"  -> "application/json"
        )
        .post(body)
        .flatMap: rsp =>
          if rsp.status == 200 then
            val choices = (Json.parse(rsp.body) \ "choices").as[JsArray]
            if choices.value.isEmpty then fuccess(None)
            else
              val value = Json.parse((choices(0) \ "message" \ "content").as[JsString].value)
              if value.isInstanceOf[JsObject] then fuccess(Some(value.as[JsObject]))
              else fufail(s"error: ${rsp.status} ${rsp.body.take(200)}")
          else fufail(s"error: ${rsp.status} ${rsp.body.take(200)}")
        .recover: e =>
          logger.error(e.getMessage)
          None

  trait UblogAutomodPrompt
