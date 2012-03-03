package lila

import ornicar.scalalib._

package object system
    extends OrnicarValidation
    with OrnicarCommon
    //with OrnicarNonEmptyLists
    with scalaz.Strings
    with scalaz.Booleans {

  implicit def addPP[A](a: A) = new {
    def pp[A] = a ~ println
  }

  def parseIntOption(str: String): Option[Int] = try {
    Some(java.lang.Integer.parseInt(str))
  }
  catch {
    case e: NumberFormatException ⇒ None
  }

  val MoveString = """^([a-h][1-8]) ([a-h][1-8])$""".r
}
