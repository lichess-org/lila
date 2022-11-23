package lila.practice

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.study.Chapter
import lila.common.Iso

object BSONHandlers:

  import PracticeProgress.{ ChapterNbMoves, NbMoves }

  private given Iso.StringIso[StudyChapterId] = Iso.string(StudyChapterId.apply, _.value)
  private given BSONHandler[ChapterNbMoves]   = typedMapHandler[StudyChapterId, NbMoves]

  given BSONHandler[PracticeProgress.Id]      = stringAnyValHandler(_.value, PracticeProgress.Id.apply)
  given BSONDocumentHandler[PracticeProgress] = Macros.handler
