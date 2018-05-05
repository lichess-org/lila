package lila.user

import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import scala.math.{ pow, BigInt }
import org.apache.commons.codec.binary.Base32

case class TotpSecret(val secret: Array[Byte]) {
  override def toString = "TotpSecret(****************)"

  def base32: String = new Base32().encodeAsString(secret)

  def totp(period: Long): String = {
    // Loosely based on scala-totp-auth:
    // https://github.com/marklister/scala-totp-auth/blob/master/src/main/scala/Authenticator.scala

    val msg = BigInt(period).toByteArray.reverse.padTo(8, 0.toByte).reverse

    val hmac = Mac.getInstance("HmacSha1")
    hmac.init(new SecretKeySpec(secret, "RAW"))
    val hash = hmac.doFinal(msg)

    val offset = hash(hash.length - 1) & 0xf

    val binary: Long = ((hash(offset) & 0x7f) << 24 |
      (hash(offset + 1) & 0xff) << 16 |
      (hash(offset + 2) & 0xff) << 8 |
      (hash(offset + 3) & 0xff))

    val otp = binary % pow(10, TotpSecret.digits).toLong
    ("0" * TotpSecret.digits + otp.toString).takeRight(TotpSecret.digits)
  }

  def verify(token: String): Boolean = {
    val period = System.currentTimeMillis / 30000
    (-TotpSecret.window to TotpSecret.window).map(skew => totp(period + skew)).contains(token)
  }
}

object TotpSecret {
  // requires clock precision of at least window * 30 seconds
  private val window = 3
  // number of digits in token
  private val digits = 6

  def apply(base32: String) = new TotpSecret(new Base32().decode(base32))

  def random: TotpSecret = {
    val secret = new Array[Byte](10)
    new SecureRandom().nextBytes(secret)
    apply(secret)
  }
}
