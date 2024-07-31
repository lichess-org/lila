package lila.rating

import reactivemongo.api.bson.BSONHandler

import lila.core.perf.PerfId
import lila.db.dsl.given

object BSONHandlers:

  given perfTypeIdHandler: BSONHandler[PerfType] =
    summon[BSONHandler[PerfId]].as[PerfType](
      id => PerfType.byId.get(id).err(s"Unknown perf id $id"),
      _.id
    )

  given perfTypeKeyHandler: BSONHandler[PerfType] =
    perfKeyHandler.as[PerfType](PerfType(_), _.key)
