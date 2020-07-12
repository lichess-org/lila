package lila.relay

import org.joda.time.DateTime
import reactivemongo.api.bson._
import reactivemongo.akkastream.{ cursorProducer, AkkaStreamCursor }
import reactivemongo.api.ReadPreference

import lila.db.dsl._

final private class RelayRepo(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._

  def scheduled =
    coll.ext
      .find(
        $doc(
          selectors scheduled true
        )
      )
      .sort($sort asc "startsAt")
      .list[Relay]()

  def ongoing =
    coll.ext
      .find(
        $doc(
          selectors ongoing true
        )
      )
      .sort($sort asc "startedAt")
      .list[Relay]()

  private[relay] def officialCursor(batchSize: Int): AkkaStreamCursor[Relay] =
    coll.ext
      .find(selectors officialOption true)
      .sort($sort desc "startsAt")
      .batchSize(batchSize)
      .cursor[Relay](ReadPreference.secondaryPreferred)

  private[relay] object selectors {
    def officialOption(v: Boolean) = $doc("official" -> v.option(true))
    def scheduled(official: Boolean) =
      officialOption(official) ++ $doc(
        "startsAt" $gt DateTime.now.minusHours(1),
        "startedAt" $exists false
      )
    def ongoing(official: Boolean) =
      officialOption(official) ++ $doc(
        "startedAt" $exists true,
        "finished" -> false
      )
    def finished(official: Boolean) =
      officialOption(official) ++ $doc(
        "startedAt" $exists true,
        "finished" -> true
      )
  }
}
