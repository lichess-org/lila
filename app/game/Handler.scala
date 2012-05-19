package lila
package game

import scalaz.effects._

abstract class Handler(gameRepo: GameRepo) {

  protected def attempt[A](
    fullId: String,
    action: Pov ⇒ Valid[IO[A]]): IO[Valid[A]] =
    fromPov(fullId) { pov ⇒ action(pov).sequence }

  protected def attemptRef[A](
    ref: PovRef,
    action: Pov ⇒ Valid[IO[A]]): IO[Valid[A]] =
    fromPov(ref) { pov ⇒ action(pov).sequence }

  protected def fromPov[A](ref: PovRef)(op: Pov ⇒ IO[Valid[A]]): IO[Valid[A]] =
    fromPov(gameRepo pov ref)(op)

  protected def fromPov[A](fullId: String)(op: Pov ⇒ IO[Valid[A]]): IO[Valid[A]] =
    fromPov(gameRepo pov fullId)(op)

  protected def fromPov[A](povIO: IO[Option[Pov]])(op: Pov ⇒ IO[Valid[A]]): IO[Valid[A]] =
    povIO flatMap { povOption ⇒
      povOption.fold(
        pov ⇒ op(pov),
        io { "No such game".failNel }
      )
    }
}
