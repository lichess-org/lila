package lila.common

import io.mola.galimatias.{ StrictErrorHandler, URL, URLParsingSettings }
import scala.util.Try

object url:

  val parser = URLParsingSettings.create.withErrorHandler(StrictErrorHandler.getInstance)

  def parse(str: String): Try[URL] = Try(URL.parse(parser, str))

  // https://example.com/path/to/resource -> https://example.com
  def origin(url: String): String =
    val pathBegin = url.indexOf('/', 8)
    if pathBegin == -1 then url else url.slice(0, pathBegin)
