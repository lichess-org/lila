package lila.irwin

import reactivemongo.bson._
import lila.db.dsl._

object BSONHandlers {

  import IrwinReport._
  private implicit val MoveReportBSONHandler = Macros.handler[MoveReport]
  private implicit val GameReportBSONHandler = Macros.handler[GameReport]
  implicit val ReportBSONHandler = Macros.handler[IrwinReport]
}
