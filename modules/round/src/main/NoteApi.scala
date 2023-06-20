package lila.round

import reactivemongo.api.bson.*
import reactivemongo.api.ReadPreference

import lila.db.dsl.{ *, given }
import lila.game.Game

final class NoteApi(coll: Coll)(using Executor):

  def collName  = coll.name
  val noteField = "t"

  def get(gameId: GameId, userId: UserId): Fu[String] =
    coll.primitiveOne[String]($id(makeId(gameId, userId)), noteField) dmap (~_)

  def set(gameId: GameId, userId: UserId, text: String) = {
    if (text.isEmpty) coll.delete.one($id(makeId(gameId, userId)))
    else
      coll.update.one(
        $id(makeId(gameId, userId)),
        $set(noteField -> text),
        upsert = true
      )
  }.void

  def byGameIds(gameIds: Seq[GameId], userId: UserId): Fu[Map[GameId, String]] =
    coll.byIds(gameIds.map(makeId(_, userId)), ReadPreference.secondaryPreferred) map { docs =>
      (for
        doc    <- docs
        gameId <- doc.getAsOpt[GameId]("_id")
        note   <- doc.string(noteField)
      yield (gameId, note)).toMap
    }

  private def makeId(gameId: GameId, userId: UserId) = s"$gameId$userId"
