package lila.irwin

import lila.db.dsl._
import lila.db.BSON
import reactivemongo.bson._

object BSONHandlers {

  import IrwinReport._

  private implicit val MoveReportBSONHandler = new BSON[MoveReport] {

    private val activation = "a"
    private val rank = "r"
    private val ambiguity = "m"
    private val odds = "o"
    private val loss = "l"

    def reads(r: BSON.Reader) = MoveReport(
      activation = r intD activation,
      rank = r intO rank,
      ambiguity = r intD ambiguity,
      odds = r intD odds,
      loss = r intD loss
    )

    def writes(w: BSON.Writer, o: MoveReport) = BSONDocument(
      activation -> w.intO(o.activation),
      rank -> o.rank.flatMap(w.intO),
      ambiguity -> w.intO(o.ambiguity),
      odds -> w.intO(o.odds),
      loss -> w.intO(o.loss)
    )
  }

  private implicit val GameReportBSONHandler = Macros.handler[GameReport]
  private implicit val PvBSONHandler = nullableHandler[Int, BSONInteger]
  implicit val ReportBSONHandler = Macros.handler[IrwinReport]

  private implicit val RequestOriginBSONHandler: BSONHandler[BSONString, IrwinRequest.Origin] =
    new BSONHandler[BSONString, IrwinRequest.Origin] {
      import IrwinRequest.Origin, Origin._
      def read(bs: BSONString) = bs.value match {
        case "moderator" => Moderator
        case "report" => Report
        case "tournament" => Tournament
        case "leaderboard" => Leaderboard
        case _ => sys error s"Invalid origin ${bs.value}"
      }
      def write(x: Origin) = BSONString(x.key)
    }
  implicit val RequestBSONHandler = Macros.handler[IrwinRequest]
}
