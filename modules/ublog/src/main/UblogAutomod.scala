package lila.ublog

import play.api.libs.json.*
import play.api.libs.ws.*
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.DefaultBodyReadables.readableAsString
import com.roundeights.hasher.Algo

import lila.core.data.Text
import lila.core.ublog.Quality
import lila.memo.SettingStore
import lila.memo.SettingStore.Text.given

// see also:
//   file://./../../../../bin/ublog-automod.mjs
//   file://./../../../../../sysadmin/prompts/ublog-system-prompt.txt

object UblogAutomod:

  private val schemaVersion = 1

  case class Assessment(
      quality: Quality,
      flagged: Option[String] = none,
      commercial: Option[String] = none,
      evergreen: Option[Boolean] = none,
      hash: Option[String] = none,
      version: Int = schemaVersion
  )

  private case class FuzzyResult(
      quality: String,
      flagged: Option[JsValue],
      commercial: Option[JsValue],
      evergreen: Option[Boolean]
  )
  private given Reads[FuzzyResult] = Json.reads[FuzzyResult]

final class UblogAutomod(
    ws: StandaloneWSClient,
    config: AutomodConfig,
    settingStore: lila.memo.SettingStore.Builder
)(using Executor):

  import UblogAutomod.*

  val promptSetting = settingStore[Text](
    "ublogAutomodPrompt",
    text = "Ublog automod prompt".some,
    default = Text("")
  )

  val temperatureSetting = settingStore[Float](
    "ublogAutomodTemperature",
    text = "Ublog automod temperature".some,
    default = 0.3
  )

  private val dedup = scalalib.cache.OnceEvery.hashCode[String](1.hour)

  private[ublog] def apply(post: UblogPost): Fu[Option[Assessment]] = post.live.so:
    val text = post.allText.take(40_000) // bin/ublog-automod.mjs, important for hash
    dedup(s"${post.id}:$text").so(assess(text))

  private def assess(userText: String): Fu[Option[Assessment]] =
    val prompt = promptSetting.get().value
    (config.apiKey.value.nonEmpty && prompt.nonEmpty).so:
      val body = Json.obj(
        "model" -> config.model,
        "temperature" -> temperatureSetting.get(),
        "messages" -> Json.arr(
          Json.obj("role" -> "system", "content" -> prompt),
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
            choices <- (Json.parse(rsp.body) \ "choices").asOpt[List[JsObject]]
            if rsp.status == 200
            best <- choices.headOption
            resultStr <- (best \ "message" \ "content").asOpt[String]
            result <- normalize(resultStr)
          yield result) match
            case None => fufail(s"${rsp.status} ${rsp.body.take(500)}")
            case Some(res) =>
              lila.mon.ublog.automod.quality(res.quality.toString).increment()
              lila.mon.ublog.automod.flagged(res.flagged.isDefined).increment()
              val hash = Algo.sha256(userText).hex.take(12) // matches ublog-automod.mjs hash
              fuccess(res.copy(hash = hash.some).some)
        .monSuccess(_.ublog.automod.request)

  private def normalize(msg: String): Option[Assessment] = // keep in sync with bin/ublog-automod.mjs
    val trimmed = msg.slice(msg.indexOf('{', msg.indexOf("</think>")), msg.lastIndexOf('}') + 1)
    Json
      .parse(trimmed)
      .asOpt[FuzzyResult]
      .flatMap: res =>
        Quality
          .fromName(res.quality)
          .map: q =>
            import Quality.*
            Assessment(
              quality = q,
              evergreen = if q == good || q == great then res.evergreen else none,
              flagged = fixString(res.flagged),
              commercial = if q != spam then fixString(res.commercial) else none
            )

  private def fixString(field: Option[JsValue]): Option[String] = // LLM make poopy
    val isBad = (v: String) => Set("none", "false", "").exists(_.equalsIgnoreCase(v))
    field match
      case Some(JsString(value)) => value.trim().some.filterNot(isBad)
      case _ => none
