package lila

import ornicar.scalalib._

package object system extends OrnicarValidation with OrnicarCommon {

  implicit def addPP[A](a: A) = new {
    def pp[A] = a~println
  }
}
