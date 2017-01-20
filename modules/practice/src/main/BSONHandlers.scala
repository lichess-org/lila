package lila.practice

import lila.db.BSON
import lila.db.dsl._
import lila.study.Study
import reactivemongo.bson._

object BSONHandlers {

  import lila.study.BSONHandlers.{ StudyIdBSONHandler }
  import StudyProgress.NbMoves

  // private implicit val nbMovesHandler = intAnyValHandler[NbMoves](_.value, NbMoves.apply)
  // private implicit val chapterNbMovesHandler = BSON.MapValue.MapHandler[NbMoves]
  // private implicit val studyProgressHandler: BSONHandler[Bdoc, StudyProgress] =
  //   isoHandler[StudyProgress, StudyProgress.ChapterNbMoves, BSONDocument]((s: StudyProgress) => s.moves, StudyProgress.apply _)(ChapterNbMovesHandler)

  // implicit val practiceProgressIdHandler = new BSONHandler[BSONString, PracticeProgress.Id] {
  //   def read(bs: BSONString): PracticeProgress.Id =
  //     if (bs.value startsWith "anon:") PracticeProgress.AnonId(bs.value drop 5)
  //     else PracticeProgress.UserId(bs.value)
  //   def write(x: PracticeProgress.Id) = BSONString(x.str)
  // }

  // import Study.idIso
  // private implicit val practiceProgressStudiesHandler =
  //   BSON.MapDocument.MapHandler[Study.Id, StudyProgress](
  //     idIso,
  //     studyProgressHandler,
  //     studyProgressHandler)

  // implicit val practiceProgressHandler = Macros.handler[PracticeProgress]
}
