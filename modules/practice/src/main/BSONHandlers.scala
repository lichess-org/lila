package lila.practice

import lila.db.dsl._
import lila.study.Chapter
import reactivemongo.bson._

object BSONHandlers {
  private implicit val nbMovesHandler: BSONHandler[BSONInteger, PracticeProgress.NbMoves] = intIsoHandler(PracticeProgress.nbMovesIso)

  private implicit val chapterIdHandler: BSONHandler[BSONString, Chapter.Id] =
    stringIsoHandler(Chapter.idIso)

  private[practice] implicit val practiceProgressIdHandler =
    stringAnyValHandler[PracticeProgress.Id](_.value, PracticeProgress.Id.apply)

  implicit val practiceProgressHandler = Macros.handler[PracticeProgress]
}
