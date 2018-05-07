package lila.common

import ornicar.scalalib.Random

case class Nonce(value: String) extends AnyVal {
  def scriptSrc = s"'nonce-$value'"
  override def toString = value
}

object Nonce {
  def random: Nonce = Nonce(Random.secureString(20))
}
