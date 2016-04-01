package lila.round

import lila.db.dsl.Coll

import reactivemongo.bson._

final class NoteApi(coll: Coll) {

  def get(gameId: String, userId: String): Fu[String] =
    coll.find(BSONDocument("_id" -> makeId(gameId, userId))).one[BSONDocument] map {
      _ flatMap (_.getAs[String]("t")) getOrElse ""
    }

  def set(gameId: String, userId: String, text: String) = {
    if (text.isEmpty) coll.remove(BSONDocument("_id" -> makeId(gameId, userId)))
    else coll.update(
      BSONDocument("_id" -> makeId(gameId, userId)),
      BSONDocument("$set" -> BSONDocument("t" -> text)),
      upsert = true)
  }.void

  private def makeId(gameId: String, userId: String) = s"$gameId$userId"
}
