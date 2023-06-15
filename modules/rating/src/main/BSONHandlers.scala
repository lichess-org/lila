package lila.rating

import lila.db.dsl.given
import reactivemongo.api.bson.BSONHandler

object BSONHandlers:

  given perfTypeIdHandler: BSONHandler[PerfType] =
    summon[BSONHandler[Perf.Id]].as[PerfType](
      id => PerfType.byId.get(id) err s"Unknown perf id $id",
      _.id
    )

  given perfTypeKeyHandler: BSONHandler[PerfType] =
    summon[BSONHandler[Perf.Key]].as[PerfType](
      key => PerfType(key) err s"Unknown perf type $key",
      _.key
    )
