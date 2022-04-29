package lila.api

import scala.util.Try
import play.api.libs.json.Json
import io.mola.galimatias.{ StrictErrorHandler, URL, URLParsingSettings }

object ExternalEngine {
  case class Raw(
      maybeUrl: Option[String],
      maybeName: Option[String],
      maxThreads: Option[Int],
      maxHash: Option[Long],
      variants: Option[String],
      officialStockfish: Boolean,
  ) {
    def prompt: Option[Prompt] =
      for {
        unvalidatedUrl <- maybeUrl
        url <- Try {
          URL.parse(
            URLParsingSettings.create.withErrorHandler(StrictErrorHandler.getInstance),
            unvalidatedUrl
          )
        }.toOption
        if url.scheme == "ws" || url.scheme == "wss"
        name <- maybeName
      } yield Prompt(
        url = url,
        name = name,
        maxThreads = maxThreads | 1,
        maxHash = maxHash,
        variants = variants.map(_.split(",")),
        officialStockfish = officialStockfish,
      )
  }

  case class Prompt(
      url: URL,
      name: String,
      maxThreads: Int,
      maxHash: Option[Long],
      variants: Option[Array[String]],
      officialStockfish: Boolean
  ) {
    private def host: Option[String] = Option(url.host).map(_.toString)

    def origin: String = s"${url.scheme}://${~host}"

    def insecure = url.scheme == "ws" && !host.exists(h => List("localhost", "[::1]", "127.0.0.1").has(h))

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
