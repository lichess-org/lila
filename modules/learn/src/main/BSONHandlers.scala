package lila.learn

import reactivemongo.api.bson._

import lila.db.dsl._

object BSONHandlers {

  import StageProgress.Score

  implicit private val ScoreHandler: BSONHandler[Score] =
    intAnyValHandler[Score](_.value, Score.apply)
  implicit private val StageProgressHandler: BSONHandler[StageProgress] =
    isoHandler[StageProgress, Vector[Score]](
      (s: StageProgress) => s.scores,
      StageProgress.apply _,
    )

  implicit val LearnProgressIdHandler: BSONHandler[LearnProgress.Id] =
    stringAnyValHandler[LearnProgress.Id](_.value, LearnProgress.Id.apply)
  implicit val LearnProgressHandler: BSONDocumentHandler[LearnProgress] =
    Macros.handler[LearnProgress]
}
