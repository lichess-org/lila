package lila.round

import lila.core.notify.{ NotifyApi, NotificationContent }
import lila.core.timeline.{ GameEnd as TLGameEnd, Propagate }

final private class RoundNotifier(
    isUserPresent: (Game, UserId) => Fu[Boolean],
    notifyApi: NotifyApi
)(using Executor):

  def gameEnd(game: Game)(color: Color) =
    if !game.aborted then
      game.player(color).userId.foreach { userId =>
        lila.common.Bus.pub:
          Propagate(
            TLGameEnd(
              fullId = game.fullIdOf(color),
              opponent = game.player(!color).userId,
              win = game.winnerColor.map(color ==),
              perf = game.perfKey
            )
          ).toUser(userId)
        isUserPresent(game, userId).foreach:
          case false =>
            notifyApi.notifyOne(
              userId,
              NotificationContent.GameEnd(
                game.fullIdOf(color),
                game.opponent(color).userId,
                game.wonBy(color)
              )
            )
          case _ =>
      }
