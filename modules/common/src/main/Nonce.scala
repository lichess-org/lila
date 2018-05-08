package lila.common

import play.api.mvc.RequestHeader
import ornicar.scalalib.Random

case class Nonce(value: String) extends AnyVal with StringValue {
  def scriptSrc = s"'nonce-$value'"
}

object Nonce {

  def get(real: Boolean): Nonce = if (real) random else stub
  def get(req: RequestHeader): Nonce = get(HTTPRequest isSynchronousHttp req)

  private def random: Nonce = Nonce(Random.secureString(20))
  private val stub: Nonce = Nonce("stub")
}
