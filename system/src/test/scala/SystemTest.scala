package lila.system

import org.specs2.mutable.Specification
import ornicar.scalalib.test.OrnicarValidationMatchers

import model._
import lila.chess._
import format.Visual

trait SystemTest
    extends Specification
    with OrnicarValidationMatchers
    with Fixtures {

  import util.control.Exception.allCatch
  import org.specs2.execute.{ Failure, Success, Result }
  import org.specs2.matcher._
  import scalaz.effects._
  /** matcher for an IO */
  def beIO[A](t: ⇒ A) = new Matcher[IO[A]] {

    def apply[S <: IO[A]](value: Expectable[S]) = {
      val expected = t
      (allCatch either { value.value.unsafePerformIO }).fold(
        e ⇒ result(false,
          "IO fails with " + e,
          "IO fails with " + e,
          value),
        a ⇒ result(a == expected,
          a + " is IO with value " + expected,
          a + " is not IO with value " + expected,
          value)
      )
    }
  }

  def beIO[A] = new Matcher[IO[A]] {

    def apply[S <: IO[A]](value: Expectable[S]) = {
      val performed = allCatch either { value.value.unsafePerformIO }
      result(performed.isRight,
        "IO perfoms successfully",
        "IO fails",
        value)
    }

    def like(f: PartialFunction[A, MatchResult[_]]) = this and partialMatcher(f)

    private def partialMatcher(
      f: PartialFunction[A, MatchResult[_]]) = new Matcher[IO[A]] {

      def apply[S <: IO[A]](value: Expectable[S]) = {
        (allCatch either { value.value.unsafePerformIO }).fold(
          e ⇒ result(false,
            "IO fails with " + e,
            "IO fails with " + e,
            value),
          a ⇒ {
            val res: Result = a match {
              case t if f.isDefinedAt(t) ⇒ f(t).toResult
              case _                     ⇒ Failure("function undefined")
            }
            result(res.isSuccess,
              a + " is IO[A] and " + res.message,
              a + " is not IO[A] with value " + res.message,
              value)
          }
        )
      }
    }
  }

  implicit def stringToBoard(str: String): Board = Visual << str

  implicit def richDbGame(dbGame: DbGame) = new {

    def withoutEvents: DbGame = dbGame mapPlayers (_.copy(evts = ""))

    def afterMove(orig: Pos, dest: Pos): Valid[DbGame] =
      dbGame.toChess.apply(orig, dest) map {
        case (ng, m) ⇒ dbGame.update(ng, m)
      }
  }

  def addNewLines(str: String) = "\n" + str + "\n"
}
