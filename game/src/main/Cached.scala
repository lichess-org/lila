package lila.game

import scala.concurrent.duration._

import spray.caching.{ LruCache, Cache }
import play.api.libs.json.JsObject
import play.api.libs.concurrent.Execution.Implicits._

private[game] final class Cached(gameRepo: GameRepo, ttl: Duration) {

  def nbGames: Fu[Int] = count(_.all)
  def nbMates: Fu[Int] = count(_.mate)
  def nbPopular: Fu[Int] = count(_.popular)
  def nbImported: Fu[Int] = count(_.imported)

  private implicit val coll = gameRepo.coll

  private def count(selector: Query.type ⇒ JsObject) =
    selector(Query) |> { sel ⇒ cache.fromFuture(sel)(gameRepo count sel) }

  private val cache: Cache[Int] = LruCache(timeToLive = ttl)
}
