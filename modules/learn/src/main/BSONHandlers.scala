package lila.learn

import reactivemongo.api.bson._

import lila.db.dsl._

object BSONHandlers {

  import StageProgress.Score

  implicit private val ScoreHandler = intAnyValHandler[Score](_.value, Score.apply)
  implicit private val StageProgressHandler =
    isoHandler[StageProgress, Vector[Score]](
      (s: StageProgress) => s.scores,
      StageProgress.apply _
    )

  implicit val LearnProgressIdHandler = stringAnyValHandler[LearnProgress.Id](_.value, LearnProgress.Id.apply)
  implicit val LearnProgressHandler   = Macros.handler[LearnProgress]
}
