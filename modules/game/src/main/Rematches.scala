package lila.game

import chess.Color
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext

/* Prev game ID -> Next game ID
 * The next game might not yet exist in the DB,
 * in which case the next game ID will set when
 * the game is created.
 * That mechanism is used by bots/boards who receive
 * rematch offers as challenges.
 */
final class Rematches(idGenerator: IdGenerator)(using ec: ExecutionContext):

  import Rematches.*

  private val cache = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterWrite(1 hour)
    .build[Game.Id, NextGame]()

  // challengeId -> prevGameId
  private val offeredReverseLookup = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterWrite(1 hour)
    .build[Game.Id, Game.Id]()

  def prevGameIdOffering = offeredReverseLookup.getIfPresent

  def get                          = cache.getIfPresent
  def getOffered(prev: Game.Id)    = get(prev) collect { case o: Offered => o }
  def getAccepted(prev: Game.Id)   = get(prev) collect { case a: Accepted => a }
  def getAcceptedId(prev: Game.Id) = getAccepted(prev).map(_.nextId)

  def isOffering(pov: PovRef) = getOffered(pov.gameId).exists(_.by == pov.color)

  def offer(pov: PovRef): Fu[Game.Id] = (getOffered(pov.gameId) match {
    case Some(existing) => fuccess(existing.copy(by = pov.color))
    case None           => idGenerator.game map { Offered(pov.color, _) }
  }) map { offer =>
    cache.put(pov.gameId, offer)
    offeredReverseLookup.put(offer.nextId, pov.gameId)
    offer.nextId
  }

  def drop(prev: Game.Id) =
    get(prev) collect { case Offered(_, nextId) => nextId } foreach offeredReverseLookup.invalidate
    cache.invalidate(prev)

  def accept(prev: Game.Id, next: Game.Id) =
    cache.put(prev, Accepted(next))
    offeredReverseLookup.invalidate(next)

object Rematches:

  sealed trait NextGame { val nextId: Game.Id }
  case class Offered(by: Color, nextId: Game.Id) extends NextGame // game doesn't yet exist
  case class Accepted(nextId: Game.Id)           extends NextGame // game exists
