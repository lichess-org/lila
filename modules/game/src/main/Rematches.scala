package lila.game

import chess.Color
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

/* Prev game ID -> Next game ID
 * The next game might not yet exist in the DB,
 * in which case the next game ID will set when
 * the game is created.
 * That mechanism is used by bots/boards who receive
 * rematch offers as challenges.
 */
final class Rematches(idGenerator: IdGenerator)(implicit ec: ExecutionContext) {

  import Rematches._

  private val cache = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterWrite(1 hour)
    .build[Game.ID, NextGame]()

  // challengeId -> prevGameId
  private val offeredReverseLookup = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterWrite(1 hour)
    .build[Game.ID, Game.ID]()

  def prevGameIdOffering = offeredReverseLookup.getIfPresent _

  def get                          = cache.getIfPresent _
  def getOffered(prev: Game.ID)    = get(prev) collect { case o: Offered => o }
  def getAccepted(prev: Game.ID)   = get(prev) collect { case a: Accepted => a }
  def getAcceptedId(prev: Game.ID) = getAccepted(prev).map(_.nextId)

  def isOffering(pov: PovRef) = getOffered(pov.gameId).exists(_.by == pov.color)

  def offer(pov: PovRef): Fu[Game.ID] = (getOffered(pov.gameId) match {
    case Some(existing) => fuccess(existing.copy(by = pov.color))
    case None           => idGenerator.game map { Offered(pov.color, _) }
  }) map { offer =>
    cache.put(pov.gameId, offer)
    offeredReverseLookup.put(offer.nextId, pov.gameId)
    offer.nextId
  }

  def drop(prev: Game.ID) = {
    get(prev) collect { case Offered(_, nextId) => nextId } foreach offeredReverseLookup.invalidate
    cache.invalidate(prev)
  }

  def accept(prev: Game.ID, next: Game.ID) = {
    cache.put(prev, Accepted(next))
    offeredReverseLookup.invalidate(next)
  }
}

object Rematches {

  sealed trait NextGame { val nextId: Game.ID }
  case class Offered(by: Color, nextId: Game.ID) extends NextGame // game doesn't yet exist
  case class Accepted(nextId: Game.ID)           extends NextGame // game exists
}
