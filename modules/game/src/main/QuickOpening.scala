package lila.game

import chess.opening.{ Opening, OpeningDb }
import chess.variant.Variant

import lila.core.game.Game
import lila.memo.CacheApi

// approximative and cached opening for game lists
final class QuickOpening(using Executor):

  private type MovesHash = Int
  private val maxMoves = 8

  private val cache =
    CacheApi.scaffeineNoScheduler
      .expireAfterAccess(15.minutes)
      .maximumSize(131_072)
      .build[MovesHash, Option[Opening]]()

  def of(game: Game): Option[Opening] =
    if !game.fromPosition && Variant.list.openingSensibleVariants(game.variant)
    then
      val firstMoves = game.sans.take(maxMoves)
      cache.get(firstMoves.hashCode, _ => OpeningDb.search(firstMoves).map(_.opening))
    else none
