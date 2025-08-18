package lila.report

import play.api.{ ConfigLoader, Configuration }
import play.api.libs.json.*
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.JsonBodyReadables.*

import lila.core.config.Secret
import lila.core.data.Text
import lila.common.autoconfig.AutoConfig
import lila.common.config.given

private final class Automod(ws: StandaloneWSClient, appConfig: Configuration)(using Executor):

  private val config = appConfig.get[Automod.Config]("automod")

  def request(
      userText: String,
      systemPrompt: Text,
      model: String,
      temperature: Double
  ): Fu[Option[JsObject]] =
    (config.apiKey.value.nonEmpty && systemPrompt.value.nonEmpty && userText.nonEmpty).so:
      val body = Json.obj(
        "model" -> model,
        "temperature" -> temperature,
        "max_tokens" -> 4096,
        "messages" -> Json.arr(
          Json.obj("role" -> "system", "content" -> systemPrompt.value),
          Json.obj("role" -> "user", "content" -> userText)
        )
      )
      ws.url(config.url)
        .withHttpHeaders(
          "Authorization" -> s"Bearer ${config.apiKey.value}",
          "Content-Type" -> "application/json"
        )
        .post(body)
        .flatMap: rsp =>
          (for
            choices <- (rsp.body \ "choices").asOpt[List[JsObject]]
            if rsp.status == 200
            best <- choices.headOption
            msg <- (best \ "message" \ "content").asOpt[String]
            trimmed = msg.slice(msg.indexOf('{', msg.indexOf("</think>")), msg.lastIndexOf('}') + 1)
          yield Json.parse(trimmed).asOpt[JsObject]) match
            case None => fufail(s"${rsp.status} ${(rsp.body: String).take(500)}")
            case Some(res) => fuccess(res)

private object Automod:
  case class Config(val url: String, val apiKey: Secret)
  given ConfigLoader[Config] = AutoConfig.loader[Config]
