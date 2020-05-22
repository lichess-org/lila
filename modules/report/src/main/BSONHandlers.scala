package lila.report

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._
import reactivemongo.api.bson._

object BSONHandlers {

  implicit val ReasonBSONHandler = isoHandler[Reason, String](Reason.reasonIso)
  implicit val RoomBSONHandler   = isoHandler[Room, String](Room.roomIso)
  import Report.{ Atom, Inquiry, Score }
  implicit val InquiryBSONHandler    = Macros.handler[Inquiry]
  implicit val ReporterIdBSONHandler = stringIsoHandler[ReporterId](ReporterId.reporterIdIso)
  implicit val ScoreIdBSONHandler    = doubleIsoHandler[Score](Report.scoreIso)
  implicit val AtomBSONHandler       = Macros.handler[Atom]
  implicit val ReportBSONHandler     = Macros.handler[Report]
}
