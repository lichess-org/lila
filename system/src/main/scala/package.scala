package lila

import ornicar.scalalib._

import com.novus.salat._
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers

package object system
    extends OrnicarValidation
    with OrnicarCommon
    with scalaz.NonEmptyLists
    with scalaz.Strings
    with scalaz.Lists
    with scalaz.Booleans {

  RegisterJodaTimeConversionHelpers()

  // custom salat context
  implicit val ctx = new Context {
    val name = "Lila System Context"
    override val typeHintStrategy = StringTypeHintStrategy(when = TypeHintFrequency.Never)
  }

  implicit def addPP[A](a: A) = new {
    def pp[A] = a ~ println
  }

  implicit def richerMap[A, B](m: Map[A, B]) = new {
    def +?(bp: (Boolean, (A, B))): Map[A, B] = if (bp._1) m + bp._2 else m
  }

  def parseIntOption(str: String): Option[Int] = try {
    Some(java.lang.Integer.parseInt(str))
  }
  catch {
    case e: NumberFormatException ⇒ None
  }

  val MoveString = """^([a-h][1-8]) ([a-h][1-8])$""".r
}
