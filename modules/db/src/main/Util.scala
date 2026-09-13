package lila.db

import reactivemongo.api.bson.BSONArray

import dsl.*

object Util:

  def removeEmptyArray(field: String)(doc: Bdoc): Bdoc =
    if doc.getAsOpt[BSONArray](field).exists(_.isEmpty)
    then (doc -- field)
    else doc
