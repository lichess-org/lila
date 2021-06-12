package lila.oauth2

import ornicar.scalalib.Random

case class Token(public: String, secret: String) {
  override def equals(other: Any) =
    other match {
      case Token(otherPublic, otherSecret) =>
        public == otherPublic &&
          // constant time comparison of secret
          secret.size == otherSecret.size &&
            secret.zip(otherSecret).foldLeft(0) { case (acc, (a, b)) =>
              acc | (a ^ b)
            } == 0
      case _ => false
    }

  override def hashCode = public.hashCode()

  override def toString = s"$public$secret"
}

object Token {
  private val prefix     = "li_"
  private val publicSize = 16
  private val secretSize = 16
  val size               = prefix.size + publicSize + secretSize

  def from(str: String): Token = {
    val (public, secret) = str.splitAt(prefix.size + publicSize)
    Token(public, secret)
  }

  def random() = Token(prefix + Random.secureString(publicSize), Random.secureString(secretSize))
}
