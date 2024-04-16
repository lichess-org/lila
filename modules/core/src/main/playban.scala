package lila.core
package playban

import lila.core.id.TourId
import lila.core.userId.UserId

opaque type RageSit = Int
object RageSit extends OpaqueInt[RageSit]:
  extension (a: RageSit) def counterView = a.value / 10

type BansOf            = List[UserId] => Fu[Map[UserId, Int]]
type RageSitOf         = UserId => Fu[RageSit]
type HasCurrentPlayban = UserId => Fu[Boolean]

case class Playban(userId: UserId, mins: Int, inTournament: Boolean)
case class SittingDetected(tourId: TourId, userId: UserId)
case class RageSitClose(userId: UserId)
