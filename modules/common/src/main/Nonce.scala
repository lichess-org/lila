package lila.common

import play.api.mvc.RequestHeader
import ornicar.scalalib.Random

case class Nonce(value: String) extends AnyVal with StringValue {
  def scriptSrc = s"'nonce-$value'"
}

object Nonce {

  def forRequest(req: RequestHeader): Option[Nonce] =
    HTTPRequest.isSynchronousHttp(req) option random

  def random: Nonce = Nonce(Random.secureString(20))
}
