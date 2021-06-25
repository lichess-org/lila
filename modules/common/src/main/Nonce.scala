package lila.common

case class Nonce(value: String) extends AnyVal with StringValue {
  def scriptSrc = s"'nonce-$value'"
}

object Nonce {

  def random: Nonce = Nonce(SecureRandom.nextString(24))
}
