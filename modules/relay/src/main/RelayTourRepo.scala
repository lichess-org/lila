package lila.relay

import org.joda.time.DateTime
import reactivemongo.api.bson._
import reactivemongo.api.ReadPreference

import lila.db.dsl._

final private class RelayTourRepo(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._
}
