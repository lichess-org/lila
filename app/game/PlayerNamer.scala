package lila
package game

object PlayerNamer {

  val anonPlayerName = "Anonymous"

  def apply(player: DbPlayer)(getUsername: String ⇒ String) =
    player.aiLevel.fold(
      level ⇒ "A.I. level " + level,
      (player.userId map getUsername).fold(
        username ⇒ "%s (%s)".format(username, player.elo getOrElse "?"),
        anonPlayerName)
    )
}
