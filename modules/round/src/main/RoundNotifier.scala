package lila.round

import lila.hub.actorApi.timeline.{ GameEnd => TLGameEnd, Propagate }
import lila.notify.NotifyApi

import lila.game.Game
import lila.user.User

final private class RoundNotifier(
    timeline: lila.hub.actors.Timeline,
    isUserPresent: (Game, User.ID) => Fu[Boolean],
    notifyApi: NotifyApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  def gameEnd(game: Game)(color: chess.Color) =
    if (!game.aborted) game.player(color).userId foreach { userId =>
      game.perfType foreach { perfType =>
        timeline ! (Propagate(
          TLGameEnd(
            playerId = game fullIdOf color,
            opponent = game.player(!color).userId,
            win = game.winnerColor map (color ==),
            perf = perfType.key
          )
        ) toUser userId)
      }
      isUserPresent(game, userId) foreach {
        case false =>
          notifyApi.notifyOne(
            userId,
            lila.notify.GameEnd(
              gameId = game fullIdOf color,
              opponentId = game.opponent(color).userId,
              win = game.wonBy(color)
            )
          )
        case _ =>
      }
    }
}
