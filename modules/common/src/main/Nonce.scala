package lila.common

opaque type Nonce = String

object Nonce extends OpaqueString[Nonce]:

  extension (a: Nonce) def scriptSrc = s"'nonce-${a.value}'"

  def random: Nonce = Nonce(SecureRandom.nextString(24))
