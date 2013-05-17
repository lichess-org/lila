package lila.game

trait Handler {

  protected def attempt[A](
    fullId: String,
    action: Pov ⇒ Fu[A]): Fu[A] =
    fromPov(fullId)(action)

  protected def attemptRef[A](
    ref: PovRef,
    action: Pov ⇒ Fu[A]): Fu[A] =
    fromPov(ref)(action)

  protected def fromPov[A](ref: PovRef)(op: Pov ⇒ Fu[A]): Fu[A] =
    fromPov(GameRepo pov ref)(op)

  protected def fromPov[A](fullId: String)(op: Pov ⇒ Fu[A]): Fu[A] =
    fromPov(GameRepo pov fullId)(op)

  protected def fromPov[A](povFu: Fu[Option[Pov]])(op: Pov ⇒ Fu[A]): Fu[A] =
    povFu flatMap { _.fold(fufail[A]("No such game"))(op) }
}
