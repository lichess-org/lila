package lila.round

import lila.game.Game
import lila.hub.actorApi.timeline.{ GameEnd as TLGameEnd, Propagate }
import lila.notify.{ GameEnd, NotifyApi }

final private class RoundNotifier(
    timeline: lila.hub.actors.Timeline,
    isUserPresent: (Game, UserId) => Fu[Boolean],
    notifyApi: NotifyApi
)(using Executor):

  def gameEnd(game: Game)(color: chess.Color) =
    if !game.aborted then
      game.player(color).userId.foreach { userId =>
        timeline ! Propagate(
          TLGameEnd(
            fullId = game fullIdOf color,
            opponent = game.player(!color).userId,
            win = game.winnerColor map (color ==),
            perf = game.perfType.key.value
          )
        ).toUser(userId)
        isUserPresent(game, userId).foreach:
          case false =>
            notifyApi.notifyOne(
              userId,
              GameEnd(
                game fullIdOf color,
                game.opponent(color).userId,
                Win from game.wonBy(color)
              )
            )
          case _ =>
      }
