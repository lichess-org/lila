package lila.core
package relay

import reactivemongo.api.bson.Macros.Annotations.Key

import lila.core.id.{ RelayRoundId, StudyId }

case class RoundIdName(@Key("_id") id: RelayRoundId, name: String)

case class GetCrowd(id: StudyId, promise: Promise[Int])
case class GetActiveRounds(promise: Promise[List[RoundIdName]])
