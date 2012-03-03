package lila.system

import org.specs2.mutable.Specification
import ornicar.scalalib.test.OrnicarValidationMatchers

import lila.chess._
import format.Visual

trait SystemTest
    extends Specification
    with OrnicarValidationMatchers
    with Fixtures {

  implicit def stringToBoard(str: String): Board = Visual << str

  def addNewLines(str: String) = "\n" + str + "\n"
}
