package lila
package game

import chess.Clock
import user.User

object Namer {

  def player(player: DbPlayer, withElo: Boolean = true)(getUsername: String ⇒ String) =
    player.aiLevel.fold(
      level ⇒ "A.I. level " + level,
      (player.userId map getUsername).fold(
        username ⇒ withElo.fold(
          "%s (%s)".format(username, player.elo getOrElse "?"),
          username),
        User.anonymous)
    )

  def clock(clock: Clock): String = "%d minutes/side + %d seconds/move".format(
    clock.limitInMinutes, clock.increment)
}
