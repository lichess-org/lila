package lila.relay

import org.joda.time.DateTime
import reactivemongo.bson._

import lila.db.dsl._

private final class RelayRepo(val coll: Coll) {

  import BSONHandlers._

  def scheduled = repo.coll.find($doc(
    selectors.scheduled ++ $doc("official" -> true)
  )).sort($sort asc "startsAt").list[Relay]()

  def ongoing = repo.coll.find($doc(
    selectors.ongoing ++ $doc("official" -> true)
  )).sort($sort asc "startedAt").list[Relay]()

  def finished = repo.coll.find($doc(
    selectors.finished ++ $doc("official" -> true)
  )).sort($sort desc "startedAt").list[Relay]()

  private[relay] object selectors {
    def scheduled = $doc(
      "startsAt" $gt DateTime.now.minusHours(1),
      "startedAt" $exists false
    )
    def ongoing = $doc(
      "startedAt" $exists true,
      "finished" -> false
    )
    def finished = $doc(
      "startedAt" $exists true,
      "finished" -> true
    )
  }
}
