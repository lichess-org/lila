package lila.common

import java.security.SecureRandom

import ornicar.scalalib.Random

case class Nonce(value: String) extends AnyVal {
  def scriptSrc = s"'nonce-$value'"
  override def toString = value
}

object Nonce {
  def random: Nonce = {
    val bytes = new Array[Byte](15)
    new SecureRandom().nextBytes(bytes)
    Nonce(bytes.toBase64)
  }
}
