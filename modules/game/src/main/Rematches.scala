package lila.game

import lila.core.game.PovRef

/* Prev game ID -> Next game ID
 * The next game might not yet exist in the DB,
 * in which case the next game ID will set when
 * the game is created.
 * That mechanism is used by bots/boards who receive
 * rematch offers as challenges.
 */
final class Rematches(idGenerator: IdGenerator)(using Executor):

  import Rematches.*

  private val cache = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterWrite(1.hour)
    .build[GameId, NextGame]()

  // challengeId -> prevGameId
  private val offeredReverseLookup = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterWrite(1.hour)
    .build[GameId, GameId]()

  def prevGameIdOffering = offeredReverseLookup.getIfPresent

  inline def get = cache.getIfPresent
  def getOffered(prev: GameId) = get(prev).collect { case o: NextGame.Offered => o }
  def getAccepted(prev: GameId) = get(prev).collect { case a: NextGame.Accepted => a }
  def getAcceptedId(prev: GameId) = getAccepted(prev).map(_.nextId)

  def isOffering(pov: PovRef) = getOffered(pov.gameId).exists(_.by == pov.color)

  def offer(pov: PovRef): Fu[GameId] = (getOffered(pov.gameId) match
    case Some(existing) => fuccess(existing.copy(by = pov.color))
    case None => idGenerator.game.map { NextGame.Offered(pov.color, _) }
  ).map { offer =>
    cache.put(pov.gameId, offer)
    offeredReverseLookup.put(offer.nextId, pov.gameId)
    offer.nextId
  }

  def drop(prev: GameId) =
    get(prev).collect { case NextGame.Offered(_, nextId) => nextId }.foreach(offeredReverseLookup.invalidate)
    cache.invalidate(prev)

  def accept(prev: GameId, next: GameId) =
    cache.put(prev, NextGame.Accepted(next))
    offeredReverseLookup.invalidate(next)

object Rematches:

  enum NextGame:
    val nextId: GameId
    case Offered(by: Color, nextId: GameId) // game doesn't yet exist
    case Accepted(nextId: GameId) // game exists
