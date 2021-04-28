package lila.relay

import org.joda.time.DateTime
import reactivemongo.api.bson._
import reactivemongo.api.ReadPreference

import lila.db.dsl._

final private class RelayTourRepo(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._

  def setSyncedNow(tour: RelayTour): Funit =
    coll.updateField($id(tour.id), "syncedAt", DateTime.now).void

  def setActive(tourId: RelayTour.Id, active: Boolean): Funit =
    coll.updateField($id(tourId), "active", active).void

  def lookup(local: String) = $lookup.simple(coll, "tour", local, "_id")

  // def scheduled =
  //   coll
  //     .find($doc(selectors scheduled true))
  //     .sort($sort asc "startsAt")
  //     .cursor[Relay]()
  //     .list()

  // def ongoing =
  //   coll
  //     .find($doc(selectors ongoing true))
  //     .sort($sort asc "startedAt")
  //     .cursor[Relay]()
  //     .list()

  // private[relay] def officialCursor(batchSize: Int): AkkaStreamCursor[Relay] =
  //   coll
  //     .find(selectors officialOption true)
  //     .sort($sort desc "startsAt")
  //     .batchSize(batchSize)
  //     .cursor[Relay](ReadPreference.secondaryPreferred)

  private[relay] object selectors {
    val official = $doc("official" -> true)
    val active   = $doc("active" -> true)
    val inactive = $doc("active" -> false)
    // def scheduled(official: Boolean) =
    //   officialOption(official) ++ $doc(
    //     "startsAt" $gt DateTime.now.minusHours(1),
    //     "startedAt" $exists false
    //   )
    // def ongoing(official: Boolean) =
    //   officialOption(official) ++ $doc(
    //     "startedAt" $exists true,
    //     "finished" -> false
    //   )
  }
}
