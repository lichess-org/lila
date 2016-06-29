package lila.learn

import play.api.libs.json._
import lila.common.PimpedJson._

object JSONHandlers {

  private implicit val LevelProgressScoreWriter = intAnyValWriter[LevelProgress.Score](_.value)
  private implicit val LevelProgressTriesWriter = intAnyValWriter[LevelProgress.Tries](_.value)
  implicit val LevelProgressWriter = Json.writes[LevelProgress]

  implicit val LearnProgressWriter = Json.writes[LearnProgress]
}
