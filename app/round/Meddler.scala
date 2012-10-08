package lila
package round

import game.{ DbGame, GameRepo, PovRef, Pov }

import scalaz.effects._

final class Meddler(
    gameRepo: GameRepo,
    finisher: Finisher,
    socket: Socket) {

  def forceAbort(id: String): IO[Unit] = for {
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

  def resign(pov: Pov): IO[Unit] = (finisher resign pov).fold(
    err ⇒ putStrLn(err.shows),
    ioEvents ⇒ for {
      events ← ioEvents
      _ ← io { socket.send(pov.game.id, events) }
    } yield ()
  )

  def resign(povRef: PovRef): IO[Unit] = for {
    povOption ← gameRepo pov povRef
    _ ← povOption.fold(resign, putStrLn("Cannot resign missing game " + povRef))
  } yield ()

  def finishAbandoned(game: DbGame): IO[Unit] = game.abandoned.fold(
    finisher.resign(Pov(game, game.player))
      .prefixFailuresWith("Finish abandoned game " + game.id)
      .fold(
        err ⇒ putStrLn(err.shows),
        _ map (_ ⇒ ()) // discard the events
      ),
    putStrLn("Game is not abandoned") 
  )
}
