package lila.game

import lila.db.api.$count
import tube.gameTube
import lila.memo.AsyncCache

import scala.concurrent.duration._
import play.api.libs.json.JsObject

private[game] final class Cached(ttl: Duration) {

  def nbGames: Fu[Int] = count(Query.all)
  def nbMates: Fu[Int] = count(Query.mate)
  def nbPopular: Fu[Int] = count(Query.popular)
  def nbImported: Fu[Int] = count(Query.imported)

  private val count = AsyncCache((o: JsObject) ⇒ $count(o), timeToLive = ttl)
}
