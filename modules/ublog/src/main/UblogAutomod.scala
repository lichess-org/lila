package lila.ublog

import com.typesafe.config.{ Config, ConfigFactory }
import play.api.{ ConfigLoader, Configuration }
import play.api.libs.json.*
import play.api.libs.ws.*
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.DefaultBodyReadables.readableAsString

import lila.memo.SettingStore
import lila.core.data.Text
import lila.memo.SettingStore.Text.given
import lila.core.config.Secret

case class UblogAutomodConfig(apiKey: Secret, model: String, url: String)

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

  private val cfg =
    import lila.common.config.given
    import lila.common.autoconfig.AutoConfig
    appConfig.get[UblogAutomodConfig]("ublog.automod")(AutoConfig.loader)

  def fetch(userText: String): Fu[JsObject] =
    val prompt = promptSetting.get().value
    (cfg.apiKey.value.nonEmpty && prompt.nonEmpty).so:
      val body = Json.obj(
        "model" -> cfg.model,
        // "response_format" -> "json", // not universally supported it seems
        // "temperature" -> 0.7,
        // "top_p" -> 1,
        // "frequency_penalty" -> 0,
        // "presence_penalty" -> 0
        "messages" -> Json.arr(
          Json.obj("role" -> "system", "content" -> prompt),
          Json.obj("role" -> "user", "content"   -> userText)
        )
      )
      ws.url(cfg.url)
        .withHttpHeaders(
          "Authorization" -> s"Bearer ${cfg.apiKey.value}",
          "Content-Type"  -> "application/json"
        )
        .post(body)
        .flatMap: rsp =>
          val content = for
            choices <- (Json.parse(rsp.body) \ "choices").asOpt[List[JsObject]]
            if rsp.status == 200
            best       <- choices.headOption
            contentStr <- (best \ "message" \ "content").asOpt[String]
            content    <- Json.parse(contentStr).asOpt[JsObject]
          yield content
          content.fold(fufail(s"error: ${rsp.status} ${rsp.body.take(200)}"))(fuccess)
