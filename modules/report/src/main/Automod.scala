package lila.report

import play.api.{ ConfigLoader, Configuration }
import play.api.libs.json.*
import play.api.libs.ws.{ StandaloneWSClient, StandaloneWSResponse }
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.JsonBodyReadables.*

import lila.common.autoconfig.AutoConfig
import lila.common.config.given
import lila.common.Json.given
import lila.core.config.Secret
import lila.core.data.Text
import lila.core.id.ImageId
import lila.memo.{ ImageAutomod, ImageAutomodRequest, Dimensions }
import lila.memo.SettingStore.Text.given

final class Automod(
    ws: StandaloneWSClient,
    appConfig: Configuration,
    settingStore: lila.memo.SettingStore.Builder,
    picfitApi: lila.memo.PicfitApi
)(using Executor):

  private val config = appConfig.get[Automod.Config]("automod")

  val imagePromptSetting = settingStore[Text](
    "imageAutomodPrompt",
    text = "Image automod prompt".some,
    default = Text("")
  )

  val imageModelSetting = settingStore[String](
    "imageAutomodModel",
    text = "Image automod model".some,
    default = "Qwen/Qwen2.5-VL-72B-Instruct"
  )

  lila.common.Bus.sub[ImageAutomodRequest]: req =>
    imageFlagReason(req.id, req.dim.some)
      .map: flagged =>
        picfitApi.setAutomod(req.id, ImageAutomod(flagged))

  def text(
      userText: String,
      systemPrompt: Text,
      model: String,
      temperature: Double = 0,
      maxTokens: Int = 4096
  ): Fu[Option[JsObject]] =
    List(config.apiKey.value, systemPrompt.value, userText)
      .forall(_.nonEmpty)
      .so:
        val body = Json
          .obj(
            "model" -> model,
            "temperature" -> temperature,
            "max_tokens" -> maxTokens,
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
          .map: rsp =>
            rsp -> extractJsonFromResponse(rsp)
          .flatMap:
            case (rsp, Left(err)) => fufail(s"${rsp.status} $err ${(rsp.body: String).take(500)}")
            case (_, Right(res)) => fuccess(res)

  def markdownImages(markdown: Markdown): Fu[Seq[lila.memo.PicfitImage]] =
    val ids = picfitApi.imageIds(markdown)
    picfitApi
      .byIds(ids)
      .flatMap:
        _.map: pic =>
          if pic.automod.isDefined then fuccess(pic)
          else
            for
              flagged <- imageFlagReason(pic.id, pic.dimensions)
              automod = ImageAutomod(flagged)
              _ <- picfitApi.setAutomod(pic.id, automod)
            yield pic.copy(automod = automod.some)
        .toSeq.parallel

  private def imageFlagReason(id: ImageId, dim: Option[Dimensions]): Fu[Option[String]] =
    val (apiKey, model, prompt) =
      (config.apiKey.value, imageModelSetting.get(), imagePromptSetting.get().value)
    List(apiKey, model, prompt)
      .forall(_.nonEmpty)
      .so:
        val imageUrl = picfitApi.url.automod(id, dim)
        val body = Json
          .obj(
            "model" -> model,
            "temperature" -> 0,
            "messages" -> Json.arr(
              Json.obj(
                "role" -> "user",
                "content" -> Json.arr(
                  Json.obj("type" -> "text", "text" -> prompt),
                  Json.obj("type" -> "image_url", "image_url" -> Json.obj("url" -> imageUrl))
                )
              )
            )
          )
        ws.url(config.url)
          .withHttpHeaders(
            "Authorization" -> s"Bearer $apiKey",
            "Content-Type" -> "application/json"
          )
          .withRequestTimeout(10.minutes) // I saw it timeout with the default 5min
          .post(body)
          .map(extractJsonFromResponse)
          .map(_.flatMap(_.toRight("No content in response")))
          .flatMap(_.toFuture)
          .prefixFailure(s"Automod image $id request failed")
          .map: res =>
            val flagged = ~res.boolean("flag")
            lila.mon.mod.report.automod.imageFlagged(flagged).increment()
            flagged.option:
              res.str("reason") | "No reason provided"
          .monSuccess(_.mod.report.automod.imageRequest)
          .recover:
            case err =>
              logger.error(err.getMessage, err)
              none

  private def extractJsonFromResponse(rsp: StandaloneWSResponse): Either[String, Option[JsObject]] =
    for
      _ <- Either.cond(rsp.status == 200, (), s"API error ${rsp.status}: ${(rsp.body: String).take(300)}")
      choices <- (rsp.body \ "choices").asOpt[List[JsObject]].toRight("No choices in response")
      best <- choices.headOption.toRight("Empty choices in response")
      msg <- (best \ "message" \ "content").validate[String].asEither.left.map(_.toString)
    yield
      val trimmed = msg.slice(msg.indexOf('{', msg.indexOf("</think>")), msg.lastIndexOf('}') + 1)
      Json.parse(trimmed).asOpt[JsObject]

private object Automod:
  case class Config(val url: String, val apiKey: Secret)
  given ConfigLoader[Config] = AutoConfig.loader[Config]
