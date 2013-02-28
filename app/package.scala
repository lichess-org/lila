import ornicar.scalalib

import com.novus.salat.{ Context, TypeHintFrequency, StringTypeHintStrategy }
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers

package object lila extends common.PackageObject {

  // custom salat context
  implicit val customSalatContext = new Context {
    val name = "Lila Context"
    override val typeHintStrategy = StringTypeHintStrategy(when = TypeHintFrequency.Never)
  }
  RegisterJodaTimeConversionHelpers()
}
