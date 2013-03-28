package lila.game

import lila.db.api.$count

import scala.concurrent.duration._

import spray.caching.{ LruCache, Cache }
import play.api.libs.json.JsObject
import play.api.libs.concurrent.Execution.Implicits._

private[game] final class Cached(ttl: Duration) {

  def nbGames: Fu[Int] = count(_.all)
  def nbMates: Fu[Int] = count(_.mate)
  def nbPopular: Fu[Int] = count(_.popular)
  def nbImported: Fu[Int] = count(_.imported)

  private implicit def tube = gameTube

  private def count(selector: Query.type ⇒ JsObject) =
    selector(Query) |> { sel ⇒ cache.fromFuture(sel)($count(sel)) }

  private val cache: Cache[Int] = LruCache(timeToLive = ttl)
}
