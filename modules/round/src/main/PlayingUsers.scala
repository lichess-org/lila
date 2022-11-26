package lila.round

import scala.concurrent.duration.*

import lila.user.User
import lila.common.Bus

final class PlayingUsers:

  private val playing = lila.memo.ExpireSetMemo[UserId](4 hours)

  def apply(userId: User.ID): Boolean = playing get UserId(userId)

  Bus.subscribeFun("startGame", "finishGame") {

    case lila.game.actorApi.FinishGame(game, _, _) if game.hasClock =>
      game.userIds.some.filter(_.nonEmpty) map (_ map { UserId(_) }) foreach playing.removeAll

    case lila.game.actorApi.StartGame(game) if game.hasClock =>
      game.userIds.some.filter(_.nonEmpty) map (_ map { UserId(_) }) foreach playing.putAll
  }
