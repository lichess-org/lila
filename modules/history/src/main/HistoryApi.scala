package lila.history

import org.joda.time.{ DateTime, Days }
import reactivemongo.bson._

import chess.Speed
import lila.db.api._
import lila.db.Types.Coll
import lila.game.Game
import lila.user.{ User, Perfs }

final class HistoryApi(coll: Coll) {

  import History.BSONReader

  private val slowSpeeds: Set[Speed] = Set(Speed.Slow, Speed.Unlimited)

  def add(user: User, game: Game, perfs: Perfs): Funit = {
    val isStd = game.variant.standard
    val changes = List(
      isStd.option("standard" -> perfs.standard),
      game.variant.chess960.option("chess960" -> perfs.chess960),
      game.variant.kingOfTheHill.option("kingOfTheHill" -> perfs.kingOfTheHill),
      (isStd && game.speed == Speed.Bullet).option("bullet" -> perfs.bullet),
      (isStd && game.speed == Speed.Blitz).option("blitz" -> perfs.blitz),
      (isStd && slowSpeeds(game.speed)).option("slow" -> perfs.slow),
      game.poolId.map(p => s"pools.$p" -> perfs.pool(p))
    ).flatten.map {
        case (k, p) => k -> p.intRating
      }
    val days = daysBetween(user.createdAt, game.updatedAt | game.createdAt)
    coll.update(
      BSONDocument("_id" -> user.id),
      BSONDocument("$set" -> BSONDocument(changes.map {
        case (perf, rating) => s"$perf.$days" -> BSONInteger(rating)
      })),
      upsert = true
    ).void
  }

  def daysBetween(from: DateTime, to: DateTime): Int =
    Days.daysBetween(from.withTimeAtStartOfDay, to.withTimeAtStartOfDay).getDays

  def get(userId: String): Fu[Option[History]] =
    coll.find(BSONDocument("_id" -> userId)).one[History]

  // def set(userId: String, history: Iterable[HistoryEntry]): Funit =
  //   coll.insert(BSONDocument(
  //     "_id" -> userId,
  //     "e" -> BSONArray(history map write)
  //   )).void

  // def addEntry(userId: String, entry: HistoryEntry): Funit =
  //   coll.update(
  //     BSONDocument("_id" -> userId),
  //     BSONDocument("$push" -> BSONDocument("e" -> write(entry))),
  //     upsert = true).void

  // def create(perfs: Perfs) = coll.insert(BSONDocument(
  //     "_id" -> user.id,
  //     "e" -> BSONArray(write(HistoryEntry(
  //       DateTime.now,
  //       user.perfs.global.glicko.intRating,
  //       user.perfs.global.glicko.intDeviation,
  //       Glicko.default.intRating)))
  //   )).void
}
