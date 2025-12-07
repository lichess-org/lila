package lila.common

import io.mola.galimatias.{ StrictErrorHandler, URL, URLParsingSettings }
import scala.util.Try

object url:

  val parser = URLParsingSettings.create.withErrorHandler(StrictErrorHandler.getInstance)

  def parse(str: String): Try[URL] = Try(URL.parse(parser, str))

  // https://example.com/path/to/resource -> https://example.com
  def origin(url: Url): Url =
    val pathBegin = url.value.indexOf('/', 8)
    if pathBegin == -1 then url else url.map(_.slice(0, pathBegin))

  def queryString(params: Map[String, String]) =
    params.map { (k, v) => s"$k=${java.net.URLEncoder.encode(v, "UTF-8")}" }.mkString("&")
