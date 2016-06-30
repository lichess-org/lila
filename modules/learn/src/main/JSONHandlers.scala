package lila.learn

import play.api.libs.json._
import lila.common.PimpedJson._

object JSONHandlers {

  private implicit val StageProgressScoreWriter = intAnyValWriter[StageProgress.Score](_.value)
  private implicit val StageProgressTriesWriter = intAnyValWriter[StageProgress.Tries](_.value)
  implicit val StageProgressWriter = Json.writes[StageProgress]

  implicit val LearnProgressWriter = Json.writes[LearnProgress]
}
