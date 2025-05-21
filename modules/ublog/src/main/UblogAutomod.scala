package lila.ublog

import com.typesafe.config.Config
import play.api.{ ConfigLoader, Configuration }
import play.api.libs.json.*
import play.api.libs.ws.*
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.DefaultBodyReadables.readableAsString

import lila.memo.SettingStore
import lila.core.data.Text
import lila.memo.SettingStore.Text.given
import lila.core.config.Secret

// see also:
//   file://./../../../../bin/ublog-automod.mjs
//   file://./../../../../../sysadmin/prompts/ublog-system-prompt.txt

private object UblogAutomod:

  case class Result(
      classification: String,
      flagged: Option[String],
      commercial: Option[String],
      offtopic: Option[String],
      evergreen: Option[Boolean],
      hash: Option[String] = none
  )

  private case class Config(apiKey: Secret, model: String, url: String)

  private case class FuzzyResult(
      classification: String,
      flagged: Option[JsValue],
      commercial: Option[JsValue],
      offtopic: Option[JsValue],
      evergreen: Option[Boolean]
  )
  private given Reads[FuzzyResult] = Json.reads[FuzzyResult]

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

  private val cfg: UblogAutomod.Config =
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
      val hash = java.security.MessageDigest
        .getInstance("SHA-256")
        .digest(userText.getBytes("UTF-8"))
        .map("%02x".format(_))
        .mkString
        .take(12)
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
            choices <- (Json.parse(rsp.body) \ "choices").asOpt[List[JsObject]]
            if rsp.status == 200
            best      <- choices.headOption
            resultStr <- (best \ "message" \ "content").asOpt[String]
            result    <- normalize(resultStr)
          yield result) match
            case None => fufail(s"${rsp.status} ${rsp.body.take(500)}")
            case Some(res) =>
              lila.mon.ublog.automod.classification(res.classification).increment()
              lila.mon.ublog.automod.flagged(res.flagged.isDefined).increment()
              fuccess(res.copy(hash = hash.some).some)
        .monSuccess(_.ublog.automod.request)

  private def normalize(msg: String): Option[Result] = // keep in sync with bin/ublog-automod.mjs
    val trimmed = msg.slice(msg.lastIndexOf('{'), msg.lastIndexOf('}') + 1)
    Json.parse(trimmed).asOpt[FuzzyResult].flatMap { res =>
      val fixed = Result(
        classification = res.classification,
        evergreen = res.evergreen,
        flagged = fix(res.flagged),
        commercial = fix(res.commercial),
        offtopic = fix(res.offtopic)
      )
      fixed.classification match
        case "great" | "good" => fixed.some
        case "weak"           => fixed.copy(evergreen = none).some
        case "spam"           => fixed.copy(evergreen = none, offtopic = none, commercial = none).some
        case _                => none
    }

  private def fix(field: Option[JsValue]): Option[String] = // LLM make poopy
    val bad = Set("none", "reason", "false", "")
    field match
      case Some(JsString(value)) => value.trim().toLowerCase().some.filterNot(bad)
      case Some(JsBoolean(true)) => "true".some
      case _                     => none
