package lila.relay

import org.joda.time.DateTime
import reactivemongo.api.bson._
import reactivemongo.akkastream.{ cursorProducer, AkkaStreamCursor }
import reactivemongo.api.ReadPreference

import lila.db.dsl._

final private class RelayRoundRepo(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._

  def byTourOrdered(tour: RelayTour): Fu[List[RelayRound]] =
    coll
      .find(selectors.tour(tour.id))
      .sort(sort.chrono)
      .cursor[RelayRound]()
      .list(RelayTour.maxRelays)

  def lastByTour(tour: RelayTour): Fu[Option[RelayRound]] =
    coll
      .find(selectors tour tour.id)
      .sort(sort.reverseChrono)
      .one[RelayRound]

  private[relay] object sort {
    val chrono        = $doc("createdAt" -> 1)
    val reverseChrono = $doc("createdAt" -> -1)
  }

  private[relay] object selectors {
    def tour(id: RelayTour.Id) = $doc("tourId" -> id)
  }
}
