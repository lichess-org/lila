package lila.memo

import com.typesafe.config.{ Config, ConfigFactory }
import play.api.{ ConfigLoader, Configuration }
import play.api.libs.json.*
import play.api.libs.ws.*
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.DefaultBodyReadables.readableAsString

final case class AutomodConfig(
    apikey: String,
    systemPrompt: String,
    model: Option[String],
    adversarialPrompt: Option[String],
    url: Option[String]
)

final class Automod(appConfig: Configuration, ws: StandaloneWSClient)(using Executor):

  private val defaultUrl   = "https://api.together.xyz/v1/chat/completions"
  private val defaultModel = "meta-llama/Llama-4-Maverick-17B-128E-Instruct-FP8"

  private val externalConfig =
    val f = new java.io.File("conf/automod.conf")
    if f.exists then ConfigFactory.parseFile(f).resolve()
    else ConfigFactory.empty()
  private val baseConfig =
    Configuration(externalConfig).withFallback(appConfig).get[Configuration]("automod")

  private val configs: Map[String, AutomodConfig] =
    baseConfig.subKeys.map { key =>
      key -> baseConfig.get[AutomodConfig](key)(lila.common.autoconfig.AutoConfig.loader)
    }.toMap

  def fetchSystem(automod: String, userText: String): Fu[Option[JsObject]] =
    configs.get(automod) match
      case None         => fuccess(None)
      case Some(config) => fetch(config, userText)

  def fetchAdversarial(automod: String, userText: String): Fu[Option[JsObject]] =
    configs.get(automod) match
      case None         => fuccess(None)
      case Some(config) => fetch(config, userText, config.adversarialPrompt)

  private def fetch(
      config: AutomodConfig,
      userText: String,
      systemText: Option[String] = None
  ): Fu[Option[JsObject]] =
    val body = Json.obj(
      "model" -> config.model.getOrElse(defaultModel),
      // "response_format" -> "json", // not universally supported it seems
      // "temperature" -> 0.7,
      // "top_p" -> 1,
      // "frequency_penalty" -> 0,
      // "presence_penalty" -> 0
      "messages" -> JsArray(
        List(
          Json.obj("role" -> "system", "content" -> systemText.getOrElse(config.systemPrompt)),
          Json.obj("role" -> "user", "content"   -> userText)
        )
      )
    )
    ws.url(config.url.getOrElse(defaultUrl))
      .withHttpHeaders(
        "Authorization" -> s"Bearer ${config.apikey}",
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
