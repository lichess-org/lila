package lila.study

import shogi.Centis

import lila.common.Maths

case class MoveOpts(
    write: Boolean,
    sticky: Boolean,
    promoteToMainline: Boolean,
    clock: Option[Centis]
)

object MoveOpts {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  private val default = MoveOpts(
    write = true,
    sticky = true,
    promoteToMainline = false,
    clock = none
  )

  def parse(o: JsObject): MoveOpts = (o \ "d").asOpt[MoveOpts] | default

  private def readCentis(hours: String, minutes: String, seconds: String): Option[Centis] =
    for {
      h <- hours.toIntOption
      m <- minutes.toIntOption
      cs <- seconds.toDoubleOption match {
        case Some(s) => Some(Maths.roundAt(s * 100, 0).toInt)
        case _       => none
      }
    } yield Centis(h * 360000 + m * 6000 + cs)

  private val clockHourMinuteRegex                 = """^(\d++):(\d+)$""".r
  private val clockHourMinuteSecondRegex           = """^(\d++):(\d++)[:\.](\d+)$""".r
  private val clockHourMinuteFractionalSecondRegex = """^(\d++):(\d++):(\d++\.\d+)$""".r

  def readCentis(str: String): Option[Centis] =
    str match {
      case clockHourMinuteRegex(hours, minutes)                => readCentis(hours, minutes, "0")
      case clockHourMinuteSecondRegex(hours, minutes, seconds) => readCentis(hours, minutes, seconds)
      case clockHourMinuteFractionalSecondRegex(hours, minutes, seconds) =>
        readCentis(hours, minutes, seconds)
      case _ => none
    }

  implicit val clockReader = Reads[Centis] {
    case JsNumber(centis) => JsSuccess(Centis(centis.toInt))
    case JsString(str) =>
      readCentis(str) match {
        case None         => JsError(JsonValidationError(s"Cannot parse clock from $str"))
        case Some(centis) => JsSuccess(centis)
      }
    case x => JsError(JsonValidationError(s"Cannot read clock from $x"))
  }

  implicit private val moveOptsReader: Reads[MoveOpts] = (
    (__ \ "write").readNullable[Boolean].map(_ | default.write) and
      (__ \ "sticky").readNullable[Boolean].map(_ | default.sticky) and
      (__ \ "promote").readNullable[Boolean].map(_ | default.promoteToMainline) and
      (__ \ "clock").readNullable[Centis]
  )(MoveOpts.apply _)
}
