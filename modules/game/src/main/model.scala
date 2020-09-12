package lila.game

import com.github.blemale.scaffeine.Cache

final case class Rematches(cache: Cache[Game.ID, Game.ID]) extends AnyVal {
  def of(gameId: Game.ID) = cache getIfPresent gameId
}
