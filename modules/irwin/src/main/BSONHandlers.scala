package lila.irwin

import lila.db.dsl._
import reactivemongo.bson._

object BSONHandlers {

  import IrwinReport._
  private implicit val MoveReportBSONHandler = Macros.handler[MoveReport]
  private implicit val GameReportBSONHandler = Macros.handler[GameReport]
  implicit val ReportBSONHandler = Macros.handler[IrwinReport]

  private implicit val RequestOriginBSONHandler: BSONHandler[BSONString, IrwinRequest.Origin] =
    new BSONHandler[BSONString, IrwinRequest.Origin] {
      import IrwinRequest.Origin, Origin._
      def read(bs: BSONString) = bs.value match {
        case "moderator" => Moderator
        case "report" => Report
        case "tournament" => Tournament
        case _ => sys error s"Invalid origin ${bs.value}"
      }
      def write(x: Origin) = BSONString(x.key)
    }
  implicit val RequestBSONHandler = Macros.handler[IrwinRequest]
}
