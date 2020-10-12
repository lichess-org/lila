package lila.report

import play.api.data.Form
import play.api.data.Forms.{ single, text }

import lila.common.Ints
import lila.memo.SettingStore.{ Formable, StringReader }

case class ScoreThresholds(mid: Int, high: Int)

private case class Thresholds(score: () => ScoreThresholds, slack: () => Int)

private object ReportThresholds {

  private val defaultScoreThresholds = ScoreThresholds(40, 50)

  val thresholdsIso = lila.common.Iso
    .ints(",")
    .map[ScoreThresholds](
      {
        case Ints(List(m, h)) => ScoreThresholds(m, h)
        case _                => defaultScoreThresholds
      },
      t => Ints(List(t.mid, t.high))
    )

  implicit val scoreThresholdsBsonHandler  = lila.db.dsl.isoHandler(thresholdsIso)
  implicit val scoreThresholdsStringReader = StringReader.fromIso(thresholdsIso)
  implicit val scoreThresholdsFormable =
    new Formable[ScoreThresholds](t => Form(single("v" -> text)) fill thresholdsIso.to(t))

  def makeScoreSetting(store: lila.memo.SettingStore.Builder) =
    store[ScoreThresholds](
      "reportScoreThresholds",
      default = defaultScoreThresholds,
      text = "Report score mid and high thresholds, separated with a comma.".some
    )

  def makeSlackSetting(store: lila.memo.SettingStore.Builder) =
    store[Int](
      "slackScoreThreshold",
      default = 80,
      text = "Slack score threshold. Comm reports with higher scores are notified in slack".some
    )
}
