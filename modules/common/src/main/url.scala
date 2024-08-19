package lila.common

import io.mola.galimatias.{ StrictErrorHandler, URL, URLParsingSettings }
import scala.util.Try

object url:

  val parser = URLParsingSettings.create.withErrorHandler(StrictErrorHandler.getInstance)

  def parse(str: String): Try[URL] = Try(URL.parse(parser, str))
