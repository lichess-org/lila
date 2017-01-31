package lila.evalCache

import lila.db.BSON
import lila.db.dsl._
import lila.tree.Eval._
import reactivemongo.bson._

object BSONHandlers {

  // private implicit val nbMovesHandler = intIsoHandler(PracticeProgress.nbMovesIso)
  // private implicit val chapterNbMovesHandler = BSON.MapValue.MapHandler[Chapter.Id, NbMoves]

  // implicit val practiceProgressIdHandler = stringAnyValHandler[PracticeProgress.Id](_.value, PracticeProgress.Id.apply)
  // implicit val practiceProgressHandler = Macros.handler[PracticeProgress]
}
