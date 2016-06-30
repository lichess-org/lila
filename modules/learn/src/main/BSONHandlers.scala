package lila.learn

import lila.db.BSON
import lila.db.dsl._
import reactivemongo.bson._

object BSONHandlers {

  private implicit val StageProgressScoreHandler = intAnyValHandler[StageProgress.Score](_.value, StageProgress.Score.apply)
  private implicit val StageProgressTriesHandler = intAnyValHandler[StageProgress.Tries](_.value, StageProgress.Tries.apply)
  private implicit val StageProgressBSONHandler = Macros.handler[StageProgress]

  private implicit val LearnProgressStagesHandler = BSON.MapDocument.MapHandler[StageProgress]
  implicit val LearnProgressHandler = Macros.handler[LearnProgress]
}
