package lila.game

import chess.opening.{ Opening, OpeningDb }
import chess.variant.Variant

import lila.core.game.Game
import lila.memo.CacheApi

// approximative and cached opening for game lists
final class GameOpening(using Executor):

  private type MovesHash = Int
  private val maxMoves = 8

  private val quickCache =
    CacheApi.scaffeineNoScheduler
      .expireAfterAccess(15.minutes)
      .maximumSize(131_072)
      .build[MovesHash, Option[Opening]]()

  private val fullCache =
    CacheApi.scaffeineNoScheduler
      .expireAfterAccess(5.minutes)
      .maximumSize(131_072)
      .build[GameId, Option[Opening.AtPly]]()

  def of(game: Game, full: Boolean): Option[Opening] =
    if full then fullAtPly(game).map(_.opening) else quick(game)

  def atPly(game: Game, full: Boolean): Option[Opening.AtPly] =
    if full then fullAtPly(game) else quickAtPly(game)

  def quick(game: Game): Option[Opening] =
    if !game.fromPosition && Variant.list.openingSensibleVariants(game.variant)
    then
      val firstMoves = game.sans.take(maxMoves)
      quickCache.get(firstMoves.hashCode, _ => OpeningDb.search(firstMoves).map(_.opening))
    else none

  def quickAtPly(game: Game): Option[Opening.AtPly] =
    quick(game).map(o => o.atPly(chess.Ply(o.nbMoves)))

  def fullAtPly(game: Game): Option[Opening.AtPly] =
    if game.fromPosition || !Variant.list.openingSensibleVariants(game.variant)
    then none
    else if game.isBeingPlayed || game.ply.value <= maxMoves then quickAtPly(game)
    else fullCache.get(game.id, _ => OpeningDb.search(game.sans))
