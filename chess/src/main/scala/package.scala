package lila

import ornicar.scalalib._

package object chess
    extends OrnicarValidation
    with OrnicarCommon
    with scalaz.Identitys
    with scalaz.Equals
    with scalaz.MABs
    with scalaz.Options
    with scalaz.Lists
    with scalaz.Booleans
    with scalaz.Strings
    with scalaz.NonEmptyLists
    with scalaz.Semigroups {

  object implicitFailures {
    implicit def stringToFailures(str: String): Failures = str wrapNel
  }
}
