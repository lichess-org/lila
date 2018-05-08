package lila.user

import org.apache.commons.codec.binary.Base32
import reactivemongo.bson._

import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import scala.math.{ pow, BigInt }
import User.TotpToken

case class TotpSecret(secret: Array[Byte]) extends AnyVal {
  override def toString = "TotpSecret(****************)"

  def base32: String = new Base32().encodeAsString(secret)

  def currentTotp = totp(System.currentTimeMillis / 30000)

  private def totp(period: Long): TotpToken = TotpToken {
    val msg = BigInt(period).toByteArray.reverse.padTo(8, 0.toByte).reverse

    val hmac = Mac.getInstance("HMACSHA1")
    hmac.init(new SecretKeySpec(secret, "RAW"))
    val hash = hmac.doFinal(msg)

    val offset = hash(hash.length - 1) & 0xf

    val otp = ((hash(offset) & 0x7f) << 24 |
      (hash(offset + 1) & 0xff) << 16 |
      (hash(offset + 2) & 0xff) << 8 |
      (hash(offset + 3) & 0xff))

    ("0" * TotpSecret.digits + otp.toString).takeRight(TotpSecret.digits)
  }

  def verify(token: TotpToken): Boolean = {
    val period = System.currentTimeMillis / 30000
    (-TotpSecret.window to TotpSecret.window).map(skew => totp(period + skew)).has(token)
  }
}

object TotpSecret {
  // requires clock precision of at least window * 30 seconds
  private val window = 3
  // number of digits in token
  private val digits = 6

  private val secureRandom = new SecureRandom()

  def apply(base32: String) = new TotpSecret(new Base32().decode(base32))

  def random: TotpSecret = {
    val secret = new Array[Byte](10)
    secureRandom.nextBytes(secret)
    apply(secret)
  }

  private[user] val totpSecretBSONHandler = new BSONHandler[BSONBinary, TotpSecret] {
    def read(bin: BSONBinary): TotpSecret = TotpSecret(bin.byteArray)
    def write(s: TotpSecret): BSONBinary = BSONBinary(s.secret, Subtype.GenericBinarySubtype)
  }
}
