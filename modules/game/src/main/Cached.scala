package lila.game

import scala.concurrent.duration._

import org.joda.time.DateTime
import play.api.libs.json.JsObject

import lila.db.api.$count
import lila.db.BSON._
import lila.memo.{ AsyncCache, MixedCache, MongoCache, ExpireSetMemo, Builder }
import lila.user.{ User, UidNb }
import tube.gameTube
import UidNb.UidNbBSONHandler

final class Cached(
    mongoCache: MongoCache.Builder,
    defaultTtl: FiniteDuration) {

  def nbGames: Fu[Int] = count(Query.all)
  def nbMates: Fu[Int] = count(Query.mate)
  def nbImported: Fu[Int] = count(Query.imported)
  def nbImportedBy(userId: String): Fu[Int] = count(Query imported userId)

  def nbPlaying(userId: String): Fu[Int] = countShortTtl(Query nowPlaying userId)

  private implicit val userHandler = User.userBSONHandler

  private val isPlayingSimulCache = MixedCache[String, Boolean](
    f = userId => GameRepo.countPlayingRealTime(userId) map (1 <),
    awaitTime = 10.milliseconds,
    timeToLive = 15.seconds,
    default = _ => false)

  val isPlayingSimul: String => Boolean = isPlayingSimulCache.get

  val rematch960 = new ExpireSetMemo(3.hours)

  val activePlayerUidsDay = mongoCache[Int, List[UidNb]](
    prefix = "player:active:day",
    (nb: Int) => GameRepo.activePlayersSince(DateTime.now minusDays 1, nb),
    timeToLive = 1 hour)

  val activePlayerUidsWeek = mongoCache[Int, List[UidNb]](
    prefix = "player:active:week",
    (nb: Int) => GameRepo.activePlayersSince(DateTime.now minusWeeks 1, nb),
    timeToLive = 6 hours)

  private val countShortTtl = AsyncCache[JsObject, Int](
    f = (o: JsObject) => $count(o),
    timeToLive = 5.seconds)

  private val count = mongoCache(
      prefix = "game:count",
      f = (o: JsObject) => $count(o),
      timeToLive = defaultTtl)

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
