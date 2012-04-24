import ornicar.scalalib._

import play.api.libs.json.JsValue
import play.api.libs.iteratee.{ Iteratee, Enumerator }
import play.api.libs.concurrent.Promise

import com.novus.salat._
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers

package object lila
    extends OrnicarValidation
    with OrnicarCommon
    with scalaz.NonEmptyLists
    with scalaz.Strings
    with scalaz.Lists
    with scalaz.Booleans {

  RegisterJodaTimeConversionHelpers()

  type Channel = socket.LilaEnumerator[JsValue]

  type SocketPromise = Promise[(Iteratee[JsValue, _], Enumerator[JsValue])]

  object Tick // standard actor tick

  type ValidIOEvents = Valid[scalaz.effects.IO[List[model.Event]]]

  // custom salat context
  implicit val ctx = new Context {
    val name = "Lila System Context"
    override val typeHintStrategy = StringTypeHintStrategy(when = TypeHintFrequency.Never)
  }

  def !!(msg: String) = msg.failNel

  val GameNotFound = !!("Game not found")

  def nowMillis: Double = System.currentTimeMillis
  def nowSeconds: Int = (nowMillis / 1000).toInt

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

  def parseFloatOption(str: String): Option[Float] = try {
    Some(java.lang.Float.parseFloat(str))
  }
  catch {
    case e: NumberFormatException ⇒ None
  }

  val MoveString = """^([a-h][1-8]) ([a-h][1-8])$""".r
}
