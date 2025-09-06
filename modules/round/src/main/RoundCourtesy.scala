package lila.round

import lila.pref.{ PrefApi, Pref }
import lila.core.game.FinishGame
import lila.round.RoundGame.hasChat

private final class RoundCourtesy(messenger: Messenger, prefApi: PrefApi)(using Executor):

  lila.common.Bus.sub[FinishGame]:
    case FinishGame(game, users) =>
      gameFilter(game).so:
        users.sequence.so:
          _.map(_.user).mapWithColor: (color, user) =>
            maybeSayGG(Pov(game, color), user)

  private def gameFilter(game: Game): Boolean = game.hasChat && game.playedPlies > 6

  private def maybeSayGG(pov: Pov, user: lila.core.user.User): Funit =
    val (drawn, lost) = (pov.game.drawn, pov.game.loser.exists(_.color == pov.color))
    (drawn || lost).so:
      prefApi
        .get(user, _.sayGG)
        .map:
          case Pref.SayGG.DEFEAT if lost => true
          case Pref.SayGG.DRAW if lost || drawn => true
          case _ => false
        .flatMap:
          _.so(messenger.sayGG(pov, user.id))
