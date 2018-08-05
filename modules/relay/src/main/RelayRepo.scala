package lila.relay

import org.joda.time.DateTime

import reactivemongo.bson._

import lila.db.dsl._

private final class RelayRepo(val coll: Coll) {

  import BSONHandlers._

  def scheduled: Fu[List[Relay]] = coll.find($doc(selectors scheduled true))
    .sort($sort asc "startsAt").cursor[Relay]().list

  def ongoing: Fu[List[Relay]] = coll.find($doc(selectors ongoing true))
    .sort($sort asc "startedAt").cursor[Relay]().list

  def finished: Fu[List[Relay]] = coll.find($doc(selectors finished true))
    .sort($sort desc "startedAt").cursor[Relay]().list

  private[relay] object selectors {
    def scheduled(official: Boolean) = $doc(
      "startsAt" $gt DateTime.now.minusHours(1),
      "startedAt" $exists false,
      "official" -> official.option(true)
    )

    def ongoing(official: Boolean) = $doc(
      "startedAt" $exists true,
      "finished" -> false,
      "official" -> official.option(true)
    )

    def finished(official: Boolean) = $doc(
      "startedAt" $exists true,
      "finished" -> true,
      "official" -> official.option(true)
    )
  }
}
