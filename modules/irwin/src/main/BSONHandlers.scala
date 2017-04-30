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
      def read(bs: BSONString) = bs.value.split(' ') match {
        case Array("mod", id) => Moderator(id)
        case Array("userReport", id) => UserReport(id)
        case Array("autoReport", id) => AutoReport(id)
        case Array("tour", id) => Tournament(id)
        case _ => sys error s"Invalid origin ${bs.value}"
      }
      def write(x: Origin) = BSONString(x match {
        case Moderator(id) => s"mod $id"
        case UserReport(id) => s"userReport $id"
        case AutoReport(id) => s"autoReport $id"
        case Tournament(id) => s"tour $id"
      })
    }
  implicit val RequestBSONHandler = Macros.handler[IrwinRequest]
}
