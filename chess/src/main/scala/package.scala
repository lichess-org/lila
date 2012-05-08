package lila

import ornicar.scalalib._

package object chess
    extends OrnicarValidation
    with OrnicarCommon
    with OrnicarNonEmptyLists 
    with scalaz.NonEmptyLists
    with scalaz.Strings
    with scalaz.Lists
    with scalaz.Booleans {

  type Direction = Pos ⇒ Option[Pos]
  type Directions = List[Direction]

  object implicitFailures {
    implicit def stringToFailures(str: String): Failures = str wrapNel
  }

  implicit def addPP[A](a: A) = new {
    def pp[A] = a ~ println
  }
}
