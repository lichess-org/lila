package lila.round

import lila.db.dsl._

import reactivemongo.api.bson._

final class NoteApi(coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  def collName = coll.name

  def get(gameId: String, userId: String): Fu[String] =
    coll.primitiveOne[String]($id(makeId(gameId, userId)), "t") dmap (~_)

  def set(gameId: String, userId: String, text: String) = {
    if (text.isEmpty) coll.delete.one($id(makeId(gameId, userId)))
    else
      coll.update.one(
        $id(makeId(gameId, userId)),
        $set("t" -> text),
        upsert = true
      )
  }.void

  private def makeId(gameId: String, userId: String) = s"$gameId$userId"
}
