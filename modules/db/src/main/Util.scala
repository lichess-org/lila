package lila.db

import reactivemongo.api.bson.BSONArray

import dsl.*

object Util:

  def findNextId(coll: Coll)(using Executor): Fu[Int] =
    coll
      .find($empty, $id(true).some)
      .sort($sort.desc("_id"))
      .one[Bdoc]
      .dmap:
        _.flatMap { doc =>
          doc.getAsOpt[Int]("_id").map(1 +)
        }.getOrElse(1)

  def removeEmptyArray(field: String)(doc: Bdoc): Bdoc =
    if doc.getAsOpt[BSONArray](field).exists(_.isEmpty)
    then (doc -- field)
    else doc
