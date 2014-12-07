package lila.round

import lila.db.Types.Coll

import reactivemongo.bson._

final class NoteApi(coll: Coll) {

  def get(fullId: String): Fu[String] =
    coll.find(BSONDocument("_id" -> fullId)).one[BSONDocument] map {
      _ flatMap (_.getAs[String]("t")) getOrElse ""
    }

  def set(fullId: String, text: String) = coll.update(
    BSONDocument("_id" -> fullId),
    BSONDocument("$set" -> BSONDocument("t" -> text)),
    upsert = true).void
}
