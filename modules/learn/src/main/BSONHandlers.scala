package lila.learn

import lila.db.BSON
import lila.db.dsl._
import reactivemongo.bson._

object BSONHandlers {

  private implicit val LevelProgressScoreHandler = intAnyValHandler[LevelProgress.Score](_.value, LevelProgress.Score.apply)
  private implicit val LevelProgressTriesHandler = intAnyValHandler[LevelProgress.Tries](_.value, LevelProgress.Tries.apply)
  private implicit val LevelProgressBSONHandler = Macros.handler[LevelProgress]

  private implicit val LearnProgressLevelsHandler = BSON.MapDocument.MapHandler[LevelProgress]
  implicit val LearnProgressHandler = Macros.handler[LearnProgress]
}
