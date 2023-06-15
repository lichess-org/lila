package lila.practice

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

object BSONHandlers:

  import PracticeProgress.{ ChapterNbMoves, NbMoves }

  private given BSONHandler[ChapterNbMoves] = typedMapHandler[StudyChapterId, NbMoves]

  given BSONDocumentHandler[PracticeProgress] = Macros.handler
