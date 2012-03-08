package lila

import ornicar.scalalib._

import com.novus.salat._

package object system
    extends OrnicarValidation
    with OrnicarCommon
    //with OrnicarNonEmptyLists
    with scalaz.NonEmptyLists
    with scalaz.Strings
    with scalaz.Booleans {

  // custom salat context
  implicit val ctx = new Context {
    val name = "Lila System Context"
    override val typeHintStrategy = StringTypeHintStrategy(when = TypeHintFrequency.Never)
  }

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
