package lila

import cats.data.Validated
import com.typesafe.config.Config
import org.joda.time.DateTime
import play.api.libs.json.{ JsObject, JsValue }
import scala.concurrent.duration.*
import scala.util.Try

import lila.base.*

trait Lilaisms
    extends LilaTypes
    with cats.syntax.OptionSyntax
    with cats.syntax.ListSyntax
    with ornicar.scalalib.Zeros
    with LilaLibraryExtensions:

  export Lilaisms.*
  export ornicar.scalalib.OrnicarBooleanWrapper

object Lilaisms:
  trait IntValue extends Any:
    def value: Int
    override def toString = value.toString
  trait BooleanValue extends Any:
    def value: Boolean
    override def toString = value.toString
  trait DoubleValue extends Any:
    def value: Double
    override def toString = value.toString

  trait StringValue extends Any:
    def value: String
    override def toString = value

  trait Percent extends Any:
    def value: Double
    def toInt = Math.round(value).toInt // round to closest

  // replaces Product.unapply in play forms
  def unapply[P <: Product](p: P)(using m: scala.deriving.Mirror.ProductOf[P]): Option[m.MirroredElemTypes] =
    Some(Tuple.fromProductTyped(p))

  implicit inline def toLilaJsObject(jo: JsObject): LilaJsObject = new LilaJsObject(jo)
  implicit inline def toLilaJsValue(jv: JsValue): LilaJsValue    = new LilaJsValue(jv)
