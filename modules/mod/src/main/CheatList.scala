package lila.mod

import org.joda.time.DateTime

import lila.db.dsl._
import lila.report.Mod
import lila.game.Game

final class CheatList(coll: Coll) {

  def set(game: Game, v: Boolean, mod: Mod): Funit =
    if (v) coll.insert.one($doc(
      "_id" -> game.id,
      "mod" -> mod.user.id,
      "date" -> DateTime.now
    )).void
    else coll.delete.one($id(game.id)).void

  def get(game: Game): Fu[Boolean] = coll.exists($id(game.id))
}
