package lila.practice

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.study.Chapter

object BSONHandlers:

  import PracticeProgress.{ ChapterNbMoves, NbMoves }

  private given (String => StudyChapterId) = StudyChapterId.apply

  given BSONHandler[PracticeProgress.Id]      = stringAnyValHandler(_.value, PracticeProgress.Id.apply)
  given BSONDocumentHandler[PracticeProgress] = Macros.handler
