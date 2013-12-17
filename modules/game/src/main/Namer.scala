package lila.game

import chess.Clock
import lila.user.User

object Namer {

  def player(player: Player, withRating: Boolean = true)(getUsername: String ⇒ Fu[String]): Fu[String] =
    player.aiLevel.fold(
      player.userId.fold(fuccess(User.anonymous)) { id ⇒
        getUsername(id) map { username ⇒
          withRating.fold(
            "%s (%s)".format(username, player.rating getOrElse "?"),
            username)
        }
      }) { level ⇒ fuccess("A.I. level " + level) }

  def shortClock(clock: Clock): String = "%d + %d ".format(
    clock.limitInMinutes, clock.increment)
}
