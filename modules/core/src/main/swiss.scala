package lila.core
package swiss

import reactivemongo.api.bson.Macros.Annotations.Key

import lila.core.chess.Rank
import lila.core.id.SwissId
import lila.core.userId.UserId

case class IdName(@Key("_id") id: SwissId, name: String)

trait SwissApi:
  def idNames(ids: List[SwissId]): Fu[List[IdName]]

type Ranking = Map[UserId, Rank]

case class SwissFinish(id: SwissId, ranking: Ranking)

trait SwissFeatureApi:
  def idNames: Fu[FeaturedIdNames]

case class FeaturedIdNames(created: List[IdName], started: List[IdName])
