package lila.round

import lila.common.Bus

final class PlayingUsers(using Executor):

  private val playing = scalalib.cache.ExpireSetMemo[UserId](4.hours)

  def apply(userId: UserId): Boolean = playing.get(userId)

  Bus.sub[lila.core.game.FinishGame]:
    case lila.core.game.FinishGame(game, _) if game.hasClock =>
      game.userIds.some.filter(_.nonEmpty).foreach(playing.removeAll)
  Bus.sub[lila.core.game.StartGame]:
    case lila.core.game.StartGame(game, _) if game.hasClock =>
      game.userIds.some.filter(_.nonEmpty).foreach(playing.putAll)
