package lila.practice

import lila.db.BSON
import lila.db.dsl._
import lila.study.Chapter
import reactivemongo.bson._

object BSONHandlers {

  import PracticeProgress.NbMoves

  private implicit val nbMovesHandler = intIsoHandler(PracticeProgress.nbMovesIso)

  implicit val practiceProgressIdHandler = stringAnyValHandler[PracticeProgress.Id](_.value, PracticeProgress.Id.apply)
  implicit val practiceProgressHandler = Macros.handler[PracticeProgress]
}
