package lila.api

import scala.util.Try
import cats.data.Validated
import play.api.libs.json.Json
import io.mola.galimatias.{ StrictErrorHandler, URL, URLParsingSettings }

object ExternalEngine {
  case class Raw(
      maybeUrl: Option[String],
      maybeName: Option[String],
      maxThreads: Option[Int],
      maxHash: Option[Int],
      variants: Option[String],
      officialStockfish: Boolean
  ) {
    def prompt: Validated[String, Prompt] =
      for {
        unvalidatedUrl <- maybeUrl.toValid("url required")
        url <- Try {
          EngineUrl(
            URL.parse(
              URLParsingSettings.create.withErrorHandler(StrictErrorHandler.getInstance),
              unvalidatedUrl
            )
          )
        }.toOption
          .toValid("url invalid")
          .ensure("expected ws:// or wss://")(url => url.value.scheme == "ws" || url.value.scheme == "wss")
          .ensure("secure wss:// required for remote engines")(_.secure)
        name <- maybeName.toValid("name required")
      } yield Prompt(
        url = url,
        name = name,
        maxThreads = maxThreads | 1,
        maxHash = maxHash,
        variants = variants.map(_.split(",")),
        officialStockfish = officialStockfish
      )
  }

  case class EngineUrl(value: URL) extends AnyVal {
    private def host: Option[String] = Option(value.host).map(_.toString)
    def origin: String               = s"${value.scheme}://${~host}"
    def secure = value.scheme == "wss" || host.exists(h => List("localhost", "[::1]", "127.0.0.1").has(h))
  }

  case class Prompt(
      url: EngineUrl,
      name: String,
      maxThreads: Int,
      maxHash: Option[Int],
      variants: Option[Array[String]],
      officialStockfish: Boolean
  ) {
    def toJson = Json.obj(
      "url"               -> url.toString,
      "name"              -> name,
      "maxThreads"        -> maxThreads,
      "maxHash"           -> maxHash,
      "variants"          -> variants,
      "officialStockfish" -> officialStockfish
    )
  }
}
