package lila.round

import lila.db.dsl._
import lila.game.Game
import reactivemongo.api.bson._

final class NoteApi(coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  def collName = coll.name
  val noteField = "t"

  def get(gameId: Game.ID, userId: String): Fu[String] =
    coll.primitiveOne[String]($id(makeId(gameId, userId)), noteField) dmap (~_)

  def set(gameId: Game.ID, userId: String, text: String) = {
    if (text.isEmpty) coll.delete.one($id(makeId(gameId, userId)))
    else
      coll.update.one(
        $id(makeId(gameId, userId)),
        $set(noteField -> text),
        upsert = true
      )
  }.void

  def byGameIds(gameIds: Seq[Game.ID], userId: String): Fu[Map[Game.ID, String]] =
    coll.byIds(gameIds.map(makeId(_, userId))) map { docs =>
      (for {
        doc <- docs
        noteId <- doc.getAsOpt[String]("_id")
        note <- doc.getAsOpt[String](noteField)
      } yield (noteId take Game.gameIdSize, note)).toMap
    }

  private def makeId(gameId: String, userId: String) = s"$gameId$userId"
}
