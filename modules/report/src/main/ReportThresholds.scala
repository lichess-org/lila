package lila.report

import play.api.data.Form
import play.api.data.Forms.{ single, text }

import lila.common.{ Ints, Iso }
import lila.memo.SettingStore.{ Formable, StringReader }
import reactivemongo.api.bson.BSONHandler

case class ScoreThresholds(mid: Int, high: Int)

private case class Thresholds(score: () => ScoreThresholds, discord: () => Int)

private object ReportThresholds:

  private val defaultScoreThresholds = ScoreThresholds(40, 50)

  given iso: Iso.StringIso[ScoreThresholds] = Iso
    .ints(",")
    .map[ScoreThresholds](
      {
        case Ints(List(m, h)) => ScoreThresholds(m, h)
        case _                => defaultScoreThresholds
      },
      t => Ints(List(t.mid, t.high))
    )

  given BSONHandler[ScoreThresholds]  = lila.db.dsl.isoHandler
  given StringReader[ScoreThresholds] = StringReader.fromIso
  given Formable[ScoreThresholds]     = new Formable(t => Form(single("v" -> text)) fill iso.to(t))

  def makeScoreSetting(store: lila.memo.SettingStore.Builder) =
    store[ScoreThresholds](
      "reportScoreThresholds",
      default = defaultScoreThresholds,
      text = "Report score mid and high thresholds, separated with a comma.".some
    )

  def makeDiscordSetting(store: lila.memo.SettingStore.Builder) =
    store[Int](
      "discordScoreThreshold",
      default = 80,
      text = "Discord score threshold. Comm reports with higher scores are notified in Discord".some
    )
