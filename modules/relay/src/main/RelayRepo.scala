package lidraughts.relay

import org.joda.time.DateTime
import reactivemongo.bson._

import lidraughts.db.dsl._

private final class RelayRepo(val coll: Coll) {

  import BSONHandlers._

  def scheduled = coll.find($doc(
    selectors scheduled true
  )).sort($sort asc "startsAt").list[Relay]()

  def ongoing = coll.find($doc(
    selectors ongoing true
  )).sort($sort asc "startedAt").list[Relay]()

  def finished = coll.find($doc(
    selectors finished true
  )).sort($sort desc "startedAt").list[Relay]()

  def featurable = coll.find($doc(
    selectors featurable
  )).sort($sort asc "startsAt").list[Relay](10).map {
    _.filter(_.featureNow) take 3
  }

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
    def featurable = $doc(
      "startsAt" $lt DateTime.now.plusHours(RelayForm.maxHomepageHours),
      "finished" -> false,
      "official" -> true,
      "homepageHours" $gt 0
    )
  }
}
