package lila

import ornicar.scalalib

package object cli
    extends scalalib.Validation
    with scalalib.Common
    with scalaz.NonEmptyLists
    with scalaz.Strings
    with scalaz.Lists
    with scalaz.Booleans {

  implicit def addPP[A](a: A) = new {
    def pp[A] = a ~ println
  }
}
