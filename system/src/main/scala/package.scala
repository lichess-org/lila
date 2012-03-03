package lila

import ornicar.scalalib._

package object system
    extends OrnicarValidation
    with OrnicarCommon
    with scalaz.Booleans {

  implicit def addPP[A](a: A) = new {
    def pp[A] = a ~ println
  }

  def withCharsOf(str: String, nb: Int): Option[(List[Char], String)] = {
    val size = str.size
    if (size == nb) Some((str.toList, ""))
    else if (str.size > nb) Some((str take nb toList, str drop nb))
    else None
  }

  val MoveString = """^([a-h][1-8]) ([a-h][1-8])$""".r
}
