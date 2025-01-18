package lila.rating

import reactivemongo.api.bson.BSONHandler

import lila.common.Iso
import lila.db.dsl._

object BSONHandlers {

  implicit val perfTypeIdIso: Iso.IntIso[PerfType] = Iso.int[PerfType](
    from = id => PerfType.byId get id err s"Invalid perf type id $id",
    to = pt => pt.id
  )

  implicit val perfTypeKeyIso: Iso.StringIso[PerfType] = Iso.string[PerfType](
    from = key => PerfType(key) err s"Invalid perf type key $key",
    to = pt => pt.key
  )

  implicit val perfTypeIdHandler: BSONHandler[PerfType]  = intIsoHandler(perfTypeIdIso)
  implicit val perfTypeKeyHandler: BSONHandler[PerfType] = stringIsoHandler(perfTypeKeyIso)
}
