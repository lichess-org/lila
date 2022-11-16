package lila.practice

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.study.Chapter

object BSONHandlers:

  import PracticeProgress.{ ChapterNbMoves, NbMoves, given }
  import Chapter.given

  private given BSONHandler[NbMoves]        = isoHandler
  private given BSONHandler[ChapterNbMoves] = typedMapHandler[Chapter.Id, NbMoves]

  given BSONHandler[PracticeProgress.Id]      = stringAnyValHandler(_.value, PracticeProgress.Id.apply)
  given BSONDocumentHandler[PracticeProgress] = Macros.handler
