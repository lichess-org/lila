package lila.round

import lila.common.Bus

final class PlayingUsers(using Executor):

  private val playing = scalalib.cache.ExpireSetMemo[UserId](4 hours)

  def apply(userId: UserId): Boolean = playing.get(userId)

  Bus.subscribeFun("startGame", "finishGame") {

    case lila.game.actorApi.FinishGame(game, _) if game.hasClock =>
      game.userIds.some.filter(_.nonEmpty).foreach(playing.removeAll)

    case lila.game.actorApi.StartGame(game) if game.hasClock =>
      game.userIds.some.filter(_.nonEmpty).foreach(playing.putAll)
  }
