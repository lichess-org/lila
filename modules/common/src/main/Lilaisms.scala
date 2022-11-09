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
    with ornicar.scalalib.ScalalibExtensions
    with LilaLibraryExtensions
    with LilaFutureExtensions
    with LilaJsObjectExtensions:

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
