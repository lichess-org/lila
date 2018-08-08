package lidraughts.practice

import lidraughts.db.BSON
import lidraughts.db.dsl._
import lidraughts.study.Chapter
import reactivemongo.bson._

object BSONHandlers {

  import PracticeProgress.NbMoves

  private implicit val nbMovesHandler = intIsoHandler(PracticeProgress.nbMovesIso)
  private implicit val chapterNbMovesHandler = BSON.MapValue.MapHandler[Chapter.Id, NbMoves]

  implicit val practiceProgressIdHandler = stringAnyValHandler[PracticeProgress.Id](_.value, PracticeProgress.Id.apply)
  implicit val practiceProgressHandler = Macros.handler[PracticeProgress]
}
