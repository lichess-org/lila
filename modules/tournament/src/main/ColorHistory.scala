package lila.tournament

import lila.memo.CacheApi

// positive strike -> user played straight strike games by white pieces
// negative strike -> black pieces
case class ColorHistory(strike: Int, balance: Int) extends Ordered[ColorHistory]:

  override def compare(that: ColorHistory): Int =
    if strike < that.strike then -1
    else if strike > that.strike then 1
    else if balance < that.balance then -1
    else if balance > that.balance then 1
    else 0

  def firstGetsWhite(that: ColorHistory)(fallback: () => Boolean) =
    val c = compare(that)
    c < 0 || (c == 0 && fallback())

  def inc(color: Color): ColorHistory =
    ColorHistory(
      strike = color.fold((strike + 1).atLeast(1), (strike - 1).atMost(-1)),
      balance = balance + color.fold(1, -1)
    )

  // couldn't play if both players played maxStrike blacks games before
  // or both player maxStrike games before
  def couldPlay(that: ColorHistory, maxStrike: Int): Boolean =
    (strike > -maxStrike || that.strike > -maxStrike) &&
      (strike < maxStrike || that.strike < maxStrike)

  // add some penalty for pairs when both players have played last game with same color
  // heuristics: after such pairing one streak will be always incremented
  def sameColors(that: ColorHistory): Boolean = strike.sign * that.strike.sign > 0

private case class PlayerWithColorHistory(player: Player, colorHistory: ColorHistory)

final private class ColorHistoryApi(using Executor):

  private val cache = CacheApi.scaffeine
    .expireAfterAccess(1.hour)
    .build[TourPlayerId, ColorHistory]()

  private val default = ColorHistory(0, 0)

  def get(playerId: TourPlayerId) = cache.getIfPresent(playerId) | default

  def inc(playerId: TourPlayerId, color: Color) = cache.put(playerId, get(playerId).inc(color))
