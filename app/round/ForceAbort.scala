package lila
package round

import game.{ DbGame, GameRepo }

import scalaz.effects._

final class ForceAbort(
    gameRepo: GameRepo,
    finisher: Finisher,
    socket: Socket) {

  def apply(id: String): IO[Unit] = for {
    gameOption ← gameRepo game id
    _ ← gameOption.fold(
      game ⇒ (finisher forceAbort game).fold(
        err ⇒ putStrLn(err.shows),
        ioEvents ⇒ for {
          events ← ioEvents
          _ ← io { socket.send(game.id, events) }
        } yield ()
      ),
      putStrLn("Cannot abort missing game " + id)
    )
  } yield ()
}
