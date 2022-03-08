package lila.game

import scala.concurrent.duration._

/* Prev game ID -> Next game ID
 * The next game might not yet exist in the DB,
 * in which case the next game ID will set when
 * the game is created.
 * That mechanism is used by bots/boards who receive
 * rematch offers as challenges.
 */
final class Rematches {

  import Rematches._

  private val cache = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterWrite(1 hour)
    .build[Game.ID, NextGame]()

  def get                        = cache.getIfPresent _
  def getAccepted(prev: Game.ID) = get(prev) collect { case Accepted(id) => id }
  def put                        = cache.put _
}

object Rematches {

  sealed trait NextGame { val id: Game.ID }
  case class Accepted(id: Game.ID) extends NextGame // game exists
  case class Offered(id: Game.ID)  extends NextGame // game doesn't yet exist
}
