package lila
package game

import scalaz.effects._
import akka.dispatch.Future

abstract class Handler(gameRepo: GameRepo) extends core.Futuristic {

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

  protected def fromPovFuture[A](ref: PovRef)(op: Pov ⇒ Future[Valid[A]]): Future[Valid[A]] =
    fromPovFuture(gameRepo pov ref)(op)

  protected def fromPovFuture[A](povIO: IO[Option[Pov]])(op: Pov ⇒ Future[Valid[A]]): Future[Valid[A]] =
    povIO.toFuture flatMap { povOption ⇒
      povOption.fold(
        pov ⇒ op(pov),
        Future("No such game".failNel)
      )
    }
}
