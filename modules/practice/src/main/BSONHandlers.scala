package lila.practice

import lila.db.BSON
import lila.db.dsl._
import lila.study.Chapter
import reactivemongo.bson._

object BSONHandlers {

  import lila.study.BSONHandlers.ChapterIdBSONHandler
  import PracticeProgress.NbMoves

  private implicit val nbMovesHandler = intIsoHandler(PracticeProgress.nbMovesIso)
  private implicit val chapterNbMovesHandler = BSON.MapValue.MapHandler[Chapter.Id, NbMoves]

  implicit val practiceProgressIdHandler = new BSONHandler[BSONString, PracticeProgress.Id] {
    def read(bs: BSONString): PracticeProgress.Id =
      if (bs.value startsWith "anon:") PracticeProgress.AnonId(bs.value drop 5)
      else PracticeProgress.UserId(bs.value)
    def write(x: PracticeProgress.Id) = BSONString(x.str)
  }

  implicit val practiceProgressHandler = Macros.handler[PracticeProgress]
}
