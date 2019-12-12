package lila.learn

import play.api.libs.json._

import lila.common.Json._

object JSONHandlers {

  private implicit val StageProgressScoreWriter = intAnyValWriter[StageProgress.Score](_.value)
  implicit val StageProgressWriter = Json.writes[StageProgress]

  private implicit val LearnProgressIdWriter = stringAnyValWriter[LearnProgress.Id](_.value)
  implicit val LearnProgressWriter = Json.writes[LearnProgress]
}
