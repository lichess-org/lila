package lila.game

import scala.concurrent.duration._

import org.joda.time.DateTime
import play.api.libs.json.JsObject

import lila.db.api.$count
import lila.memo.{ AsyncCache, ExpireSetMemo, Builder }
import tube.gameTube

final class Cached(defaultTtl: FiniteDuration) {

  def nbGames: Fu[Int] = count(Query.all)
  def nbMates: Fu[Int] = count(Query.mate)
  def nbImported: Fu[Int] = count(Query.imported)
  def nbImportedBy(userId: String): Fu[Int] = count(Query imported userId)

  def nbPlaying(userId: String): Fu[Int] = countShortTtl(Query nowPlaying userId)

  val rematch960 = new ExpireSetMemo(3.hours)

  val activePlayerUidsDay = AsyncCache(
    (nb: Int) => GameRepo.activePlayersSince(DateTime.now minusDays 1, nb),
    timeToLive = 1 hour)

  val activePlayerUidsWeek = AsyncCache(
    (nb: Int) => GameRepo.activePlayersSince(DateTime.now minusWeeks 1, nb),
    timeToLive = 6 hours)

  private val count = countTtl(defaultTtl)
  private val countShortTtl = countTtl(3.seconds)

  private def countTtl(ttl: FiniteDuration) =
    AsyncCache((o: JsObject) => $count(o), timeToLive = ttl)

  object Divider {

    private val cache = Builder.size[String, chess.Division](5000)
    val empty = chess.Division(none[Int], none[Int])

    def apply(game: Game, initialFen: Option[String]): chess.Division = {
      Option(cache getIfPresent game.id) | {
        val div = chess.Replay.boards(
          moveStrs = game.pgnMoves,
          initialFen = initialFen
        ).toOption.fold(empty)(chess.Divider.apply)
        cache.put(game.id, div)
        div
      }
    }
  }
}
