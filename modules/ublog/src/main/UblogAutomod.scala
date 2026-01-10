package lila.ublog

import play.api.libs.json.*
import com.roundeights.hasher.Algo

import lila.core.data.Text
import lila.core.ublog.Quality
import lila.memo.SettingStore
import lila.memo.SettingStore.Text.given
import lila.report.Automod

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

private final class UblogAutomod(
    automod: lila.report.Automod,
    settingStore: lila.memo.SettingStore.Builder,
    picfitApi: lila.memo.PicfitApi
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

  private[ublog] def apply(post: UblogPost, temperature: Double = 0): Fu[Option[Assessment]] = post.live.so:
    val assessImages = automod.markdownImages:
      post.markdown.map: markdown =>
        val mainImageAsMarkdown = post.image.so(i => s"![](${picfitApi.url.automod(i.id, none)})\n")
        mainImageAsMarkdown + markdown
    val assessText =
      val userText = post.allText.take(40_000) // match bin/ublog-automod.mjs hash
      automod
        .text(
          userText = userText,
          systemPrompt = promptSetting.get(),
          model = modelSetting.get(),
          temperature = temperature
        )
        .map:
          _.flatMap(normalize).so: res =>
            lila.mon.ublog.automod.quality(res.quality.toString).increment()
            lila.mon.ublog.automod.flagged(res.flagged.isDefined).increment()
            res.copy(hash = Algo.sha256(userText).hex.take(12).some).some // match bin/ublog-automod.mjs hash
        .monSuccess(_.ublog.automod.request)
    assessImages
      .zip(assessText)
      .map: (imgs, text) =>
        val flags = imgs.flatMap(_.automod).flatMap(_.flagged)
        text.map(t => t.copy(flagged = (t.flagged ++ flags).mkString(", ").some))

  private def normalize(rsp: JsObject): Option[Assessment] =
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
