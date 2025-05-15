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

private object UblogAutomod:

  case class Config(apiKey: Secret, model: String, url: String)

  case class Result(
      classification: String,
      flagged: Option[String],
      commercial: Option[String],
      offtopic: Option[String],
      evergreen: Option[Boolean]
  )
  private given Reads[Result] = Json.reads[Result]

  private[ublog] val classifications = List("spam", "weak", "good", "great")

final class UblogAutomod(
    ws: StandaloneWSClient,
    appConfig: Configuration,
    settingStore: lila.memo.SettingStore.Builder
)(using Executor):

  import UblogAutomod.*

  val promptSetting = settingStore[Text](
    "ublogAutomodPrompt",
    text = "Ublog automod prompt".some,
    default = Text("")
  )

  private val cfg =
    import lila.common.config.given
    import lila.common.autoconfig.AutoConfig
    appConfig.get[UblogAutomod.Config]("ublog.automod")(using AutoConfig.loader)

  private val dedup = scalalib.cache.OnceEvery.hashCode[String](1.hour)

  private[ublog] def apply(post: UblogPost): Fu[Option[Result]] = post.live.so:
    val text = post.allText.take(40_000) // roughly 10k tokens
    dedup(s"${post.id}:$text").so(fetchText(text))

  private def fetchText(userText: String): Fu[Option[Result]] =
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
          (for
            choices <- (Json.parse(rsp.body.pp) \ "choices").asOpt[List[JsObject]]
            if rsp.status == 200
            best      <- choices.headOption
            resultStr <- (best \ "message" \ "content").asOpt[String]
            trimmed = resultStr.slice(resultStr.indexOf('{'), resultStr.lastIndexOf('}') + 1)
            result <- Json.parse(trimmed).asOpt[Result]
            if (classifications ++ List("quality", "phenomenal")).contains(result.classification)
          // temporarily permit "quality" and "phenomenal" as the prompt is versioned outside of this git
          yield result) match
            case None => fufail(s"${rsp.status} ${rsp.body.take(500)}")
            case Some(res) =>
              lila.mon.ublog.automod.classification(res.classification).increment()
              lila.mon.ublog.automod.flagged(res.flagged.isDefined).increment()
              fuccess(res.some)
        .monSuccess(_.ublog.automod.request)
