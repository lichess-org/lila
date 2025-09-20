package lila.core
package relay

import lila.core.id.{ StudyId, RelayRoundId }
import reactivemongo.api.bson.Macros.Annotations.Key

case class RoundIdName(@Key("_id") id: RelayRoundId, name: String)

case class GetCrowd(id: StudyId, promise: Promise[Int])
case class GetActiveRounds(promise: Promise[List[RoundIdName]])
