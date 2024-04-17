package lila.round

import lila.core.timeline.{ GameEnd as TLGameEnd, Propagate }
import lila.core.notify.{ GameEnd, NotifyApi }
import lila.core.chess.Win

final private class RoundNotifier(
    isUserPresent: (Game, UserId) => Fu[Boolean],
    notifyApi: NotifyApi
)(using Executor):

  def gameEnd(game: Game)(color: chess.Color) =
    if !game.aborted then
      game.player(color).userId.foreach { userId =>
        lila.common.Bus.named.timeline(
          Propagate(
            TLGameEnd(
              fullId = game.fullIdOf(color),
              opponent = game.player(!color).userId,
              win = game.winnerColor.map(color ==),
              perf = game.perfKey
            )
          ).toUser(userId)
        )
        isUserPresent(game, userId).foreach:
          case false =>
            notifyApi.notifyOne(
              userId,
              GameEnd(
                game.fullIdOf(color),
                game.opponent(color).userId,
                Win.from(game.wonBy(color))
              )
            )
          case _ =>
      }
