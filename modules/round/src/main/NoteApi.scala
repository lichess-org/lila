package lila.round

import lila.db.dsl._

import reactivemongo.bson._

final class NoteApi(coll: Coll) {

  def get(gameId: String, userId: String): Fu[String] =
    coll.primitiveOne[String]($id(makeId(gameId, userId)), "t") map (~_)

  def set(gameId: String, userId: String, text: String) = {
    if (text.isEmpty) coll.remove($id(makeId(gameId, userId)))
    else coll.update(
      $id(makeId(gameId, userId)),
      $set("t" -> text),
      upsert = true
    )
  }.void

  private def makeId(gameId: String, userId: String) = s"$gameId$userId"
}
