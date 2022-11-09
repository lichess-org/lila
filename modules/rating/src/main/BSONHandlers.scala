package lila.rating

import lila.db.dsl.*
import lila.common.Iso
import reactivemongo.api.bson.BSONHandler

object BSONHandlers:

  given Iso[Int, PerfType] with
    val from = id => PerfType.byId get id err s"Invalid perf type id $id"
    val to   = _.id

  given Iso[String, PerfType] with
    val from = key => PerfType(key) err s"Invalid perf type key $key"
    val to   = _.key

  given perfTypeIdHandler: BSONHandler[PerfType]  = intIsoHandler
  given perfTypeKeyHandler: BSONHandler[PerfType] = stringIsoHandler
