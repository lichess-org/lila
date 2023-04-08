package lila.practice

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.common.Iso

object BSONHandlers:

  import PracticeProgress.{ ChapterNbMoves, NbMoves }

  private given Iso.StringIso[StudyChapterId] = Iso.string(StudyChapterId(_), _.value)
  private given BSONHandler[ChapterNbMoves]   = typedMapHandler[StudyChapterId, NbMoves]

  given BSONDocumentHandler[PracticeProgress] = Macros.handler
