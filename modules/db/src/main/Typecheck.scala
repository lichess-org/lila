package lila.db

import api._
import play.api.libs.iteratee._

object Typecheck {

  private def iteratee[A](stop: Boolean): Iteratee[A, Unit] = {

    def step(input: Input[A], nb: Int): Iteratee[A, Unit] = {
      if (nb % 1000 == 0) loginfo("typechecked " + nb)
      input match {
        case Input.EOF                       => Done((), Input.EOF)
        case Input.Empty | Input.El(Some(_)) => Cont(i => step(i, nb + 1))
        case Input.El(_) =>
          if (stop) Error("Type error", input)
          else Cont(i => step(i, nb + 1))
      }
    }
    Cont(i => step(i, 1))
  }

  def apply[A: TubeInColl]: Fu[String] = apply(true)

  def apply[A: TubeInColl](stop: Boolean): Fu[String] =
    $cursor[A]($select.all).enumerate() run iteratee(stop) inject "Typecheck complete"
}
