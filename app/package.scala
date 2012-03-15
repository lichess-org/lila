package lila

import ornicar.scalalib._

package object http
    extends OrnicarValidation
    with OrnicarCommon
    with scalaz.NonEmptyLists
    with scalaz.Strings
    with scalaz.Booleans {

  implicit def addPP[A](a: A) = new {
    def pp[A] = a ~ println
  }
}
