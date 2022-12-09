package lila.rating

import lila.db.dsl.{ *, given }
import lila.common.Iso
import reactivemongo.api.bson.BSONHandler

object BSONHandlers:

  given Iso[Int, PerfType] with
    val from = id => PerfType.byId get id err s"Invalid perf type id $id"
    val to   = _.id

  given perfTypeIdHandler: BSONHandler[PerfType] = intIsoHandler

  given perfTypeKeyHandler: BSONHandler[PerfType] =
    summon[BSONHandler[Perf.Key]].as[PerfType](
      key => PerfType(key) err s"Unknown perf type $key",
      _.key
    )
