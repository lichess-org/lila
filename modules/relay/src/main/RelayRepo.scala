package lila.relay

import org.joda.time.DateTime
import reactivemongo.api.bson._
import reactivemongo.akkastream.{ cursorProducer, AkkaStreamCursor }
import reactivemongo.api.ReadPreference

import lila.db.dsl._

final private class RelayRepo(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._

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

  def byTour(tour: RelayTour): Fu[List[Relay]] =
    coll
      .find($doc("tourId" -> tour.id))
      .sort($sort desc "startsAt")
      .cursor[Relay]()
      .list(RelayTour.maxRelays)

  private[relay] object selectors {
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
    def finished(official: Boolean) =
      // officialOption(official) ++
      $doc(
        "startedAt" $exists true,
        "finished" -> true
      )
  }
}
