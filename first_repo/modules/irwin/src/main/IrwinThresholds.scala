package lila.irwin

import lila.memo.SettingStore.{ Formable, StringReader }
import play.api.data.Form
import play.api.data.Forms.{ single, text }
import lila.common.Ints

case class IrwinThresholds(report: Int, mark: Int)

private object IrwinThresholds {

  private val defaultThresholds = IrwinThresholds(88, 95)

  val thresholdsIso = lila.common.Iso
    .ints(",")
    .map[IrwinThresholds](
      {
        case Ints(List(r, m)) => IrwinThresholds(r, m)
        case _                => defaultThresholds
      },
      t => Ints(List(t.report, t.mark))
    )

  implicit val thresholdsBsonHandler  = lila.db.dsl.isoHandler(thresholdsIso)
  implicit val thresholdsStringReader = StringReader.fromIso(thresholdsIso)
  implicit val thresholdsFormable =
    new Formable[IrwinThresholds](t => Form(single("v" -> text)) fill thresholdsIso.to(t))

  def makeSetting(store: lila.memo.SettingStore.Builder) =
    store[IrwinThresholds](
      "irwinThresholds",
      default = defaultThresholds,
      text = "Irwin report and mark thresholds, separated with a comma. Set to 101 to disable.".some
    )
}
