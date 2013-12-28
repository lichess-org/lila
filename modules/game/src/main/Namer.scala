package lila.game

import chess.Clock

import lila.user.User

object Namer {

  def players(game: Game, withRatings: Boolean = true)(implicit getUsername: String ⇒ Fu[String]): Fu[(String, String)] =
    player(game.firstPlayer, withRatings) zip player(game.secondPlayer, withRatings)

  def player(player: Player, withRating: Boolean = true)(implicit getUsername: String ⇒ Fu[String]): Fu[String] =
    player.aiLevel.fold(
      player.userId.fold(fuccess(User.anonymous)) { id ⇒
        getUsername(id) map { username ⇒
          withRating.fold(
            s"$username (${player.rating getOrElse "?"})",
            username)
        }
      }) { level ⇒ fuccess("A.I. level " + level) }

  def shortClock(clock: Clock): String = "%d + %d ".format(
    clock.limitInMinutes, clock.increment)
}
