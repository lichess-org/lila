package lila.game

import play.api.libs.concurrent.Execution.Implicits._

abstract class Handler(gameRepo: GameRepo) {

  protected def attempt[A](
    fullId: String,
    action: Pov ⇒ Valid[Fu[A]]): Fu[Valid[A]] =
    fromPov(fullId) { pov ⇒ action(pov).sequence }

  protected def attemptRef[A](
    ref: PovRef,
    action: Pov ⇒ Valid[Fu[A]]): Fu[Valid[A]] =
    fromPov(ref) { pov ⇒ action(pov).sequence }

  protected def fromPov[A](ref: PovRef)(op: Pov ⇒ Fu[Valid[A]]): Fu[Valid[A]] =
    fromPov(gameRepo pov ref)(op)

  protected def fromPov[A](fullId: String)(op: Pov ⇒ Fu[Valid[A]]): Fu[Valid[A]] =
    fromPov(gameRepo pov fullId)(op)

  protected def fromPov[A](povFu: Fu[Option[Pov]])(op: Pov ⇒ Fu[Valid[A]]): Fu[Valid[A]] =
    povFu flatMap { _.fold(fuccess(!![A]("No such game")))(op) }

  protected def fromPovFu[A](ref: PovRef)(op: Pov ⇒ Fu[Valid[A]]): Fu[Valid[A]] =
    fromPovFu(gameRepo pov ref)(op)

  protected def fromPovFu[A](povFu: Fu[Option[Pov]])(op: Pov ⇒ Fu[Valid[A]]): Fu[Valid[A]] =
    povFu flatMap { _.fold(fuccess(!![A]("No such game")))(op) }
}
