package lila.report

import play.api.{ ConfigLoader, Configuration }
import play.api.libs.json.*
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.JsonBodyReadables.*
import scala.util.matching.Regex.quote

import lila.core.config.Secret
import lila.core.data.Text
import lila.common.autoconfig.AutoConfig
import lila.common.config.given
import lila.memo.SettingStore.Text.given
import lila.core.id.ImageId

final class Automod(
    ws: StandaloneWSClient,
    appConfig: Configuration,
    settingStore: lila.memo.SettingStore.Builder,
    picfitApi: lila.memo.PicfitApi,
    picfitUrl: lila.memo.PicfitUrl
)(using Executor):

  private val config = appConfig.get[Automod.Config]("automod")

  private val imageIdRe =
    raw"""(?i)!\[(?:[^\n\]]*+)\]\(${quote(
        picfitUrl.origin
      )}[^)\s]+[?&]path=([a-z]\w+:[a-z0-9]{12}:[a-z0-9]{8}\.\w{3,4})[^)]*\)""".r

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

  def text(
      userText: String,
      systemPrompt: Text,
      model: String,
      temperature: Double = 0,
      maxTokens: Int = 4096
  ): Fu[Option[JsObject]] =
    (config.apiKey.value.nonEmpty && systemPrompt.value.nonEmpty && userText.nonEmpty).so:
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

  def markdownImages(markdown: Markdown): Fu[Seq[lila.memo.PicfitImage]] =
    val idToUrl = imageIdRe
      .findAllMatchIn(markdown.value)
      .map: m =>
        val id = lila.core.id.ImageId(m.group(1))
        id -> picfitUrl.forAutomod(id)
      .toMap
    picfitApi
      .byIds(idToUrl.keys)
      .flatMap:
        _.map: pic =>
          if pic.automod.isDefined then fuccess(pic)
          else
            for
              flagged <- idToUrl.get(pic.id).so(imageFlagReason)
              automod = lila.memo.ImageAutomod(flagged)
              _ <- picfitApi.setAutomod(pic.id, automod)
            yield pic.copy(automod = automod.some)
        .toSeq.parallel

  private def imageFlagReason(imageUrl: String): Fu[Option[String]] =
    (config.apiKey.value.nonEmpty && imagePromptSetting.get().value.nonEmpty).so:
      val body = Json
        .obj(
          "model" -> imageModelSetting.get(),
          "temperature" -> 0,
          "messages" -> Json.arr(
            Json.obj(
              "role" -> "user",
              "content" -> Json.arr(
                Json.obj("type" -> "text", "text" -> imagePromptSetting.get().value),
                Json.obj("type" -> "image_url", "image_url" -> Json.obj("url" -> imageUrl))
              )
            )
          )
        )
      ws.url(config.url)
        .withHttpHeaders(
          "Authorization" -> s"Bearer ${config.apiKey.value}",
          "Content-Type" -> "application/json"
        )
        .post(body)
        .map: rsp =>
          for
            choices <- (rsp.body \ "choices").asOpt[List[JsObject]]
            if rsp.status == 200
            best <- choices.headOption
            msg <- (best \ "message" \ "content").asOpt[String]
            trimmed = msg.slice(msg.indexOf('{', msg.indexOf("</think>")), msg.lastIndexOf('}') + 1)
            res <- Json.parse(trimmed).asOpt[JsObject]
            if ~res.boolean("flag")
          yield
            lila.mon.mod.report.automod.imageFlagged.increment()
            ~res.str("reason")
        .monSuccess(_.mod.report.automod.imageRequest)

private object Automod:
  case class Config(val url: String, val apiKey: Secret)
  given ConfigLoader[Config] = AutoConfig.loader[Config]
