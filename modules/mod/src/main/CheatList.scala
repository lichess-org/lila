package lidraughts.mod

import org.joda.time.DateTime

import lidraughts.db.dsl._
import lidraughts.report.Mod
import lidraughts.game.Game

final class CheatList(coll: Coll) {

  def set(game: Game, v: Boolean, mod: Mod): Funit =
    if (v) coll.insert($doc(
      "_id" -> game.id,
      "mod" -> mod.user.id,
      "date" -> DateTime.now
    )).void
    else coll.remove($id(game.id)).void

  def get(game: Game): Fu[Boolean] = coll.exists($id(game.id))
}
