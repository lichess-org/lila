package lila.learn

import play.api.libs.json._

import lila.common.Json._

object JSONHandlers {

  implicit private val StageProgressScoreWriter: Writes[StageProgress.Score] =
    intAnyValWriter[StageProgress.Score](_.value)
  implicit val StageProgressWriter: OWrites[StageProgress] = Json.writes[StageProgress]

  implicit private val LearnProgressIdWriter: Writes[LearnProgress.Id] =
    stringAnyValWriter[LearnProgress.Id](_.value)
  implicit val LearnProgressWriter: OWrites[LearnProgress] = Json.writes[LearnProgress]
}
