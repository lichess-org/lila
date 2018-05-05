package lila.user

import java.security.SecureRandom
import scala.math.BigInt

case class TotpSecret(val secret: Array[Byte]) {
  override def toString = "TotpSecret(****************)"

  def base32: String = {
    new String(BigInt(secret).toString.toCharArray.map(_.asDigit).map(TotpSecret.base(_)))
  }
}

object TotpSecret {
  private val base = ('A' to 'Z') ++ ('2' to '7')

  def apply(base32: String) = new TotpSecret(base32.map(base.indexOf(_)).foldLeft(0: BigInt)((a, b) => a * 32 + b).toByteArray)

  def random: TotpSecret = {
    val secret = new Array[Byte](10)
    new SecureRandom().nextBytes(secret)
    apply(secret)
  }
}
