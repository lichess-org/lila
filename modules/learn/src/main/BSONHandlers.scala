package lila.learn

import reactivemongo.api.bson._

import lila.db.dsl._

object BSONHandlers {

  import StageProgress.Score

  private given BSONHandler[Score] = intAnyValHandler[Score](_.value, Score.apply)
  private given BSONHandler[StageProgress] = isoHandler[StageProgress, Vector[Score]](
    (s: StageProgress) => s.scores,
    StageProgress.apply _
  )

  given BSONHandler[LearnProgress.Id] = stringAnyValHandler[LearnProgress.Id](_.value, LearnProgress.Id.apply)
  given BSONHandler[LearnProgress]    = Macros.handler[LearnProgress]
}
