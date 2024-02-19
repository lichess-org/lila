package lila.relay

import reactivemongo.api.bson.Macros.Annotations.Key
import ornicar.scalalib.ThreadLocalRandom

case class RelayGroup(@Key("_id") id: RelayRoundId, tours: List[RelayTour.Id])

object RelayGroup:
  opaque type Id = String
  object Id extends OpaqueString[Id]
  def makeId = RelayGroup.Id(ThreadLocalRandom nextString 8)
  case class WithTours(group: RelayGroup, tours: List[RelayTour])
