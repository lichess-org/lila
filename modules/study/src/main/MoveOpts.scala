package lila.study

import chess.Centis

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

  implicit val clockReader = Reads[Centis] {
    case JsNumber(centis) => JsSuccess(Centis(centis.toInt))
    case JsString(str) =>
      CommentParser.readCentis(str) match {
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
