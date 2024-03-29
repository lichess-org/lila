package lila.hub
package swiss

import reactivemongo.api.bson.Macros.Annotations.Key

case class IdName(@Key("_id") id: StudyId, name: StudyName)

trait SwissApi:
  def idNames(ids: List[SwissId]): Fu[List[IdName]]

type Ranking = Map[UserId, Rank]

case class SwissFinish(id: SwissId, ranking: Ranking)
