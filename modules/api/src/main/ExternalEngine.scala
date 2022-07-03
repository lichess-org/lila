package lila.api

import io.mola.galimatias.URL
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json

import lila.common.Form._

object ExternalEngine {

  case class EngineUrl(value: URL) extends AnyVal {
    private def host: Option[String] = Option(value.host).map(_.toHostString)
    def origin: String               = s"${value.scheme}://${~host}"
    def secure = value.scheme == "wss" || host
      .exists(h => List("localhost", "[::1]", "[::]", "127.0.0.1", "0.0.0.0").has(h))
  }

  val form = Form(
    mapping(
      "url" -> url.field
        .transform[EngineUrl](EngineUrl, _.value)
        .verifying("expected ws:// or wss://", url => url.value.scheme == "ws" || url.value.scheme == "wss")
        .verifying("secure wss:// required for remote engines", _.secure),
      "secret"            -> nonEmptyText,
      "name"              -> nonEmptyText(maxLength = 140),
      "maxThreads"        -> optional(number(min = 1)),
      "maxHash"           -> optional(number(min = 0)),
      "variants"          -> optional(strings separator ","),
      "officialStockfish" -> optional(boolean)
    )(Prompt.apply)(Prompt.unapply)
  )

  case class Prompt(
      url: EngineUrl,
      secret: String,
      name: String,
      maxThreads: Option[Int],
      maxHash: Option[Int],
      variants: Option[List[String]],
      officialStockfish: Option[Boolean]
  ) {
    def toJson = Json.obj(
      "url"               -> url.value.toString,
      "secret"            -> secret,
      "name"              -> name,
      "maxThreads"        -> (~maxThreads atLeast 1),
      "maxHash"           -> maxHash,
      "variants"          -> variants,
      "officialStockfish" -> ~officialStockfish
    )
  }
}
