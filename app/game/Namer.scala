package lila.app
package game

import chess.Clock
import user.User

object Namer {

  def player(player: DbPlayer, withElo: Boolean = true)(getUsername: String ⇒ String) =
    player.aiLevel.fold(
      (player.userId map getUsername).fold(User.anonymous) { username ⇒
        withElo.fold("%s (%s)".format(username, player.elo getOrElse "?"), username)
      }
    ) { level ⇒ "A.I. level " + level }

  def clock(clock: Clock): String = "%d minutes/side + %d seconds/move".format(
    clock.limitInMinutes, clock.increment)

  def shortClock(clock: Clock): String = "%d + %d ".format(
    clock.limitInMinutes, clock.increment)
}
