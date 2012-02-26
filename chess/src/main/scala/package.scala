package lila

import ornicar.scalalib._

package object chess
    extends OrnicarValidation
    with OrnicarCommon {

  object implicitFailures {
    implicit def stringToFailures(str: String): Failures = str wrapNel
  }
}
