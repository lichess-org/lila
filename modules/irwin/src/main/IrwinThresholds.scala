package lila.irwin

import lila.memo.SettingStore.{ Formable, StringReader }
import play.api.data.Form
import play.api.data.Forms.{ single, text }

case class IrwinThresholds(report: Int, mark: Int)

private object IrwinThresholds {
  val defaultThresholds = IrwinThresholds(88, 95)

  val thresholdsIso = lila.common.Iso[String, IrwinThresholds](
    str =>
      {
        str.split(',').map(_.trim) match {
          case Array(rs, ms) =>
            for {
              report <- rs.toIntOption
              mark   <- ms.toIntOption
            } yield IrwinThresholds(report, mark)
          case _ => none
        }
      } | defaultThresholds,
    t => s"${t.report}, ${t.mark}"
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
