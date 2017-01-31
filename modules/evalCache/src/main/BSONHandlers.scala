package lila.evalCache

import reactivemongo.bson._

import chess.format.{ FEN, Uci }
import lila.db.BSON
import lila.db.dsl._
import lila.tree.Eval._

object BSONHandlers {

  import EvalCacheEntry._

  // private implicit val nbMovesHandler = intIsoHandler(PracticeProgress.nbMovesIso)
  // private implicit val chapterNbMovesHandler = BSON.MapValue.MapHandler[Chapter.Id, NbMoves]

  // implicit val practiceProgressIdHandler = stringAnyValHandler[PracticeProgress.Id](_.value, PracticeProgress.Id.apply)
  private implicit val FenBSONHandler = stringAnyValHandler[FEN](_.value, FEN.apply)
  private implicit val TrustBSONHandler = doubleAnyValHandler[Trust](_.value, Trust.apply)

  implicit val UciHandler = new BSONHandler[BSONString, Uci] {
    def read(bs: BSONString): Uci = Uci(bs.value) err s"Bad UCI: ${bs.value}"
    def write(x: Uci) = BSONString(x.uci)
  }
  implicit val ScoreHandler = new BSONHandler[BSONInteger, Score] {
    private val sillyThreshold = math.pow(10, 6).toInt
    def read(bs: BSONInteger): Score =
      if (bs.value > sillyThreshold) Score.mate(Mate(bs.value - sillyThreshold))
      else if (bs.value < -sillyThreshold) Score.mate(Mate(bs.value + sillyThreshold))
      else Score cp Cp(bs.value)
    def write(x: Score) = BSONInteger(x.value match {
      case Left(cp)                => cp.value
      case Right(mate) if mate > 0 => mate.value + sillyThreshold
      case Right(mate) if mate < 0 => mate.value - sillyThreshold
    })
  }

  implicit val evalHandler = Macros.handler[Eval]
  implicit val entryHandler = Macros.handler[EvalCacheEntry]
}
