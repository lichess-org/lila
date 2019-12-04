package lila.practice

import lila.db.BSON
import lila.db.dsl._
import lila.study.BSONHandlers._
import reactivemongo.api.bson._

object BSONHandlers {

  import PracticeProgress.{ NbMoves, ChapterNbMoves }

  private implicit val nbMovesHandler: BSONHandler[NbMoves] = intIsoHandler(PracticeProgress.nbMovesIso)
  private implicit val chapterNbMovesHandler: BSONHandler[ChapterNbMoves] =
    implicitly[BSONHandler[Map[lila.study.Chapter.Id, NbMoves]]]

  implicit val practiceProgressIdHandler = stringAnyValHandler[PracticeProgress.Id](_.value, PracticeProgress.Id.apply)
  implicit val practiceProgressHandler = Macros.handler[PracticeProgress]
}
