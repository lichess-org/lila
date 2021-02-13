package lila.tournament

import chess.Color
import scala.concurrent.duration._

import lila.memo.CacheApi

//positive strike -> user played straight strike games by white pieces
//negative strike -> black pieces
case class ColorHistory(strike: Int, balance: Int) extends Ordered[ColorHistory] {

  override def compare(that: ColorHistory): Int = {
    if (strike < that.strike) -1
    else if (strike > that.strike) 1
    else if (balance < that.balance) -1
    else if (balance > that.balance) 1
    else 0
  }

  def firstGetsWhite(that: ColorHistory)(fallback: () => Boolean) = {
    val c = compare(that)
    c < 0 || (c == 0 && fallback())
  }

  def inc(color: Color): ColorHistory =
    copy(
      strike = color.fold((strike + 1) atLeast 1, (strike - 1) atMost -1),
      balance = balance + color.fold(1, -1)
    )

  //couldn't play if both players played maxStrike blacks games before
  //or both player maxStrike games before
  def couldPlay(that: ColorHistory, maxStrike: Int): Boolean =
    (strike > -maxStrike || that.strike > -maxStrike) &&
      (strike < maxStrike || that.strike < maxStrike)

  //add some penalty for pairs when both players have played last game with same color
  //heuristics: after such pairing one streak will be always incremented
  def sameColors(that: ColorHistory): Boolean = strike.sign * that.strike.sign > 0
}

case class PlayerWithColorHistory(player: Player, colorHistory: ColorHistory)

final class ColorHistoryApi(cacheApi: CacheApi) {

  private val cache = cacheApi.scaffeine
    .expireAfterAccess(1 hour)
    .build[Player.ID, ColorHistory]()

  def default = ColorHistory(0, 0)

  def get(playerId: Player.ID) = cache.getIfPresent(playerId) | default

  def inc(playerId: Player.ID, color: Color) = cache.put(playerId, get(playerId) inc color)
}
