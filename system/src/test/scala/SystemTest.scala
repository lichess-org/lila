package lila.system

import org.specs2.mutable.Specification
import ornicar.scalalib.test.OrnicarValidationMatchers

trait SystemTest
    extends Specification
    with OrnicarValidationMatchers
    with Fixtures {
}
