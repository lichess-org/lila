package lila.irwin

import play.api.data.Form
import play.api.data.Forms.{ single, text }
import reactivemongo.api.bson.BSONHandler
import scalalib.Iso

import lila.core.data.Ints
import lila.memo.SettingStore.{ Formable, StringReader }

case class IrwinThresholds(report: Int, mark: Int)

private object IrwinThresholds:

  private val defaultThresholds = IrwinThresholds(101, 101)

  given iso: Iso.StringIso[IrwinThresholds] = lila.common.Iso
    .ints(",")
    .map[IrwinThresholds](
      {
        case Ints(List(r, m)) => IrwinThresholds(r, m)
        case _ => defaultThresholds
      },
      t => Ints(List(t.report, t.mark))
    )

  given BSONHandler[IrwinThresholds] = lila.db.dsl.isoHandler
  given StringReader[IrwinThresholds] = StringReader.fromIso
  given Formable[IrwinThresholds] = new Formable(t => Form(single("v" -> text)).fill(iso.to(t)))

  def makeSetting(name: String, store: lila.memo.SettingStore.Builder) =
    store[IrwinThresholds](
      s"${name}Thresholds",
      default = defaultThresholds,
      text = s"${name} report and mark thresholds, separated with a comma. Set to 101 to disable.".some
    )
