package lidraughts.round

import lidraughts.hub.actorApi.timeline.{ Propagate, GameEnd => TLGameEnd }
import lidraughts.notify.{ GameEnd, Notification, NotifyApi }

import lidraughts.game.Game
import lidraughts.user.User

private final class RoundNotifier(
    timeline: akka.actor.ActorSelection,
    isUserPresent: (Game, User.ID) => Fu[Boolean],
    notifyApi: NotifyApi
) {

  def gameEnd(game: Game)(color: draughts.Color) = {
    if (!game.aborted) game.player(color).userId foreach { userId =>
      game.perfType foreach { perfType =>
        timeline ! (Propagate(TLGameEnd(
          playerId = game fullIdOf color,
          opponent = game.player(!color).userId,
          win = game.winnerColor map (color ==),
          perf = perfType.key
        )) toUser userId)
      }
      isUserPresent(game, userId) foreach {
        case false => notifyApi.addNotification(Notification.make(
          Notification.Notifies(userId),
          GameEnd(
            GameEnd.GameId(game.id),
            game.opponent(color).userId map GameEnd.OpponentId.apply,
            game.wonBy(color) map GameEnd.Win.apply
          )
        ))
        case _ =>
      }
    }
  }
}
