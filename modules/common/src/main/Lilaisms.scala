package lila

import cats.data.Validated
import com.typesafe.config.Config
import org.joda.time.DateTime
import ornicar.scalalib.ScalalibExtensions
import play.api.libs.json.{ JsObject, JsValue }
import scala.concurrent.duration._
import scala.util.Try

import lila.base._

trait Lilaisms
    extends LilaTypes
    with cats.syntax.OptionSyntax
    with cats.syntax.ListSyntax
    with ScalalibExtensions
    with LilaPrimitiveExtensions
    with LilaLibraryExtensions
    with LilaFutureExtensions
    with LilaJsObjectExtensions:

  type StringValue = lila.base.LilaTypes.StringValue
  // type IntValue    = lila.base.LilaTypes.IntValue
  type Percent = lila.base.LilaTypes.Percent
