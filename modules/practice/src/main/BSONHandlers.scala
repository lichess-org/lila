package lila.practice

import lila.db.BSON
import lila.db.dsl._
import lila.study.{ Study, Chapter }
import reactivemongo.bson._

object BSONHandlers {

  import lila.study.BSONHandlers.{ StudyIdBSONHandler }
  import StudyProgress.NbMoves

  private implicit val nbMovesHandler = intIsoHandler(StudyProgress.nbMovesIso)
  private implicit val chapterNbMovesHandler = BSON.MapValue.MapHandler[Chapter.Id, NbMoves]
  private implicit val studyProgressHandler = BSON toDocHandler {
    isoHandler[StudyProgress, StudyProgress.ChapterNbMoves, BSONDocument]((s: StudyProgress) => s.moves, StudyProgress.apply _)(chapterNbMovesHandler)
  }

  implicit val practiceProgressIdHandler = new BSONHandler[BSONString, PracticeProgress.Id] {
    def read(bs: BSONString): PracticeProgress.Id =
      if (bs.value startsWith "anon:") PracticeProgress.AnonId(bs.value drop 5)
      else PracticeProgress.UserId(bs.value)
    def write(x: PracticeProgress.Id) = BSONString(x.str)
  }

  import Study.idIso
  private implicit val practiceProgressStudiesHandler =
    BSON.MapDocument.MapHandler[Study.Id, StudyProgress]

  implicit val practiceProgressHandler = Macros.handler[PracticeProgress]
}
