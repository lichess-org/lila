package lila.practice

import lila.db.dsl._
import lila.study.Chapter
import reactivemongo.api.bson._

object BSONHandlers {

  import PracticeProgress.{ NbMoves, ChapterNbMoves }

  private implicit val nbMovesHandler: BSONHandler[NbMoves] =
    isoHandler(PracticeProgress.nbMovesIso)
  private implicit val chapterNbMovesHandler: BSONHandler[ChapterNbMoves] =
    typedMapHandler[Chapter.Id, NbMoves](Chapter.idIso)

  implicit val practiceProgressIdHandler = stringAnyValHandler[PracticeProgress.Id](_.value, PracticeProgress.Id.apply)
  implicit val practiceProgressHandler = Macros.handler[PracticeProgress]
}
