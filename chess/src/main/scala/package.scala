package lila

import ornicar.scalalib._

package object chess extends OrnicarValidation with OrnicarCommon {

  type Direction = Pos => Option[Pos]
  type Directions = List[Direction]

  object implicitFailures {
    implicit def stringToFailures(str: String): Failures = str wrapNel
  }

  implicit def addPP[A](a: A) = new {
    def pp[A] = a ~ println
  }
}
