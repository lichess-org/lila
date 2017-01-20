package lila.practice

import lila.db.BSON
import lila.db.dsl._
import reactivemongo.bson._

object BSONHandlers {

  import StudyProgress.NbMoves

  private implicit val NbMovesHandler = intAnyValHandler[NbMoves](_.value, NbMoves.apply)
  private implicit val ChapterNbMovesHandler = BSON.MapValue.MapHandler[NbMoves]
  private implicit val StudyProgressHandler =
    isoHandler[StudyProgress, StudyProgress.ChapterNbMoves, BSONDocument](_.moves, StudyProgress.apply)(ChapterNbMovesHandler)

  implicit val PracticeProgressIdHandler = new BSONHandler[BSONString, PracticeProgress.Id] {
    def read(bs: BSONString): PracticeProgress.Id =
      if (bs.value startsWith "anon:") PracticeProgress.AnonId(bs.value drop 5)
      else PracticeProgress.UserId(bs.value)
    def write(x: PracticeProgress.Id) = BSONString(x.str)
  }
  private implicit val PracticeProgressStudiesHandler = BSON.MapValue.MapHandler[StudyProgress]
  implicit val PracticeProgressHandler = Macros.handler[PracticeProgress]
}
