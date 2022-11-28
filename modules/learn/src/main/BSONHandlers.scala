package lila.learn

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

object BSONHandlers:

  import StageProgress.Score

  private given BSONHandler[Score] = intAnyValHandler[Score](_.value, Score.apply)
  private given BSONHandler[StageProgress] = isoHandler[StageProgress, Vector[Score]](
    (s: StageProgress) => s.scores,
    StageProgress.apply
  )

  given BSONDocumentHandler[LearnProgress] = Macros.handler
