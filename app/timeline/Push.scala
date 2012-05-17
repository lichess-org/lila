package lila
package timeline

import chess.Color
import game.{ DbGame, PlayerNamer }

import scalaz.effects._

final class Push(
    entryRepo: EntryRepo,
    lobbyNotify: Entry ⇒ IO[Unit],
    getUsername: String ⇒ String) {

  def apply(game: DbGame): IO[Unit] = makeEntry(game) |> { entry ⇒
    for {
      _ ← entryRepo add entry
      _ ← lobbyNotify(entry)
    } yield ()
  }

  private def makeEntry(game: DbGame) = Entry(
    gameId = game.id,
    whiteName = usernameElo(game, Color.White),
    blackName = usernameElo(game, Color.Black),
    whiteId = userId(game, Color.White),
    blackId = userId(game, Color.Black),
    variant = game.variant.name,
    rated = game.isRated,
    clock = game.clock map (_.show))

  private def userId(game: DbGame, color: Color): Option[String] =
    (game player color).userId

  private def usernameElo(game: DbGame, color: Color): String =
    PlayerNamer(game player color)(getUsername)
}
