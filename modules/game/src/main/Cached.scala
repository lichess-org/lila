package lila.game

import scala.concurrent.duration._

import org.joda.time.DateTime
import play.api.libs.json.JsObject
import chess.variant.Variant

import lila.db.api.$count
import lila.db.BSON._
import lila.memo.{ AsyncCache, MongoCache, ExpireSetMemo, Builder }
import lila.user.{ User, UidNb }
import tube.gameTube
import UidNb.UidNbBSONHandler

final class Cached(
    mongoCache: MongoCache.Builder,
    defaultTtl: FiniteDuration) {

  def nbImportedBy(userId: String): Fu[Int] = count(Query imported userId)
  def clearNbImportedByCache(userId: String) = count.remove(Query imported userId)

  def nbPlaying(userId: String): Fu[Int] = countShortTtl(Query nowPlaying userId)

  private implicit val userHandler = User.userBSONHandler

  val rematch960 = new ExpireSetMemo(3.hours)

  val isRematch = new ExpireSetMemo(3.hours)

  // very expensive
  // val activePlayerUidsDay = mongoCache[Int, List[UidNb]](
  //   prefix = "player:active:day",
  //   (nb: Int) => GameRepo.activePlayersSince(DateTime.now minusDays 1, nb),
  //   timeToLive = 1 hour)

  private val countShortTtl = AsyncCache[JsObject, Int](
    f = (o: JsObject) => $count(o),
    timeToLive = 5.seconds)

  private val count = mongoCache(
    prefix = "game:count",
    f = (o: JsObject) => $count(o),
    timeToLive = defaultTtl)

  object Divider {

    private val cache = Builder.size[String, chess.Division](5000)

    def apply(game: Game, initialFen: Option[String]): chess.Division =
      if (!Variant.divisionSensibleVariants.contains(game.variant)) chess.Division.empty
      else Option(cache getIfPresent game.id) | {
        val div = chess.Replay.boards(
          moveStrs = game.pgnMoves,
          initialFen = initialFen,
          variant = game.variant
        ).toOption.fold(chess.Division.empty)(chess.Divider.apply)
        cache.put(game.id, div)
        div
      }
  }
}
