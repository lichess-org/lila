package lila.common

import lila.common.Random

case class Nonce(value: String) extends AnyVal with StringValue {
  def scriptSrc = s"'nonce-$value'"
}

object Nonce {

  def random: Nonce = Nonce(Random.secureString(24))
}
