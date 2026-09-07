package lila.round

import reactivemongo.api.bson.*

import lila.db.dsl.*

final class NoteApi(coll: Coll)(using Executor):

  def collName = coll.name
  val noteField = "t"

  def get(gameId: GameId, userId: UserId): Fu[String] =
    coll.secondary.primitiveOne[String]($id(makeId(gameId, userId)), noteField).dmap(~_)

  def set(gameId: GameId, userId: UserId, text: String) = {
    if text.isEmpty then coll.delete.one($id(makeId(gameId, userId)))
    else
      coll.update.one(
        $id(makeId(gameId, userId)),
        $set(noteField -> text),
        upsert = true
      )
  }.void

  val form =
    import play.api.data.Forms.*
    play.api.data.Form(single("text" -> text))

  private def makeId(gameId: GameId, userId: UserId) = s"$gameId$userId"
