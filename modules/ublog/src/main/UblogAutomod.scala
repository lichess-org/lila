package lila.ublog

import play.api.libs.json.*
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
      lockedBy: Option[UserId] = none,
      version: Int = schemaVersion
  ):
    def updateByLLM(llm: Assessment): Assessment =
      if lockedBy.isDefined then
        copy(
          flagged = flagged.orElse(llm.flagged),
          commercial = commercial.orElse(llm.commercial)
        )
      else
        llm.copy(
          quality = Quality.fromOrdinal:
            llm.quality.ordinal.atLeast(quality.ordinal)
        )

  private case class FuzzyResult(
      quality: String,
      flagged: Option[JsValue],
      commercial: Option[JsValue],
      evergreen: Option[Boolean]
  )
  private given Reads[FuzzyResult] = Json.reads[FuzzyResult]

final class UblogAutomod(
    reportApi: lila.report.ReportApi,
    settingStore: lila.memo.SettingStore.Builder
)(using Executor):

  import UblogAutomod.*

  val promptSetting = settingStore[Text](
    "ublogAutomodPrompt",
    text = "Ublog automod prompt".some,
    default = Text("")
  )

  val modelSetting = settingStore[String](
    "ublogAutomodModel",
    text = "Ublog automod model".some,
    default = "Qwen/Qwen3-235B-A22B-Thinking-2507"
  )

  private val dedup = scalalib.cache.OnceEvery.hashCode[String](1.hour)

  private[ublog] def apply(post: UblogPost, temperature: Double = 0): Fu[Option[Assessment]] = post.live.so:
    val text = post.allText.take(40_000) // bin/ublog-automod.mjs, important for hash
    dedup(s"${post.id}:$text").so(assess(text, temperature))

  private def assess(userText: String, temperature: Double): Fu[Option[Assessment]] =
    reportApi
      .automodRequest(
        userText = userText,
        systemPrompt = promptSetting.get(),
        model = modelSetting.get(),
        temperature = temperature
      )
      .map:
        _.flatMap(normalize).so: res =>
          lila.mon.ublog.automod.quality(res.quality.toString).increment()
          lila.mon.ublog.automod.flagged(res.flagged.isDefined).increment()
          res.copy(hash = Algo.sha256(userText).hex.take(12).some).some // matches ublog-automod.mjs hash
      .monSuccess(_.ublog.automod.request)

  private def normalize(rsp: JsObject): Option[Assessment] = // keep in sync with bin/ublog-automod.mjs
    rsp
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
