package lidraughts.rating

import lidraughts.db.dsl._
import lidraughts.common.Iso

object BSONHandlers {

  implicit val perfTypeIdIso = Iso.int[PerfType](
    from = id => PerfType.byId get id err s"Invalid perf type id $id",
    to = pt => pt.id
  )

  implicit val perfTypeKeyIso = Iso.string[PerfType](
    from = key => PerfType(key) err s"Invalid perf type key $key",
    to = pt => pt.key
  )

  implicit val perfTypeIdHandler = intIsoHandler(perfTypeIdIso)
  implicit val perfTypeKeyHandler = stringIsoHandler(perfTypeKeyIso)
}
