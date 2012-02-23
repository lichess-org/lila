package lila

import ornicar.scalalib.test.OrnicarValidationMatchers
import org.specs2.mutable.Specification

trait LilaSpec
    extends Specification
    with OrnicarValidationMatchers {

  implicit def anyMatchers[A](a: A) = {
    def equals(b: A) = a mustEqual b
  }

}
