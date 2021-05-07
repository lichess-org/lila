package lila.user

import org.apache.commons.codec.binary.Base32
import reactivemongo.api.bson._

import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.nio.ByteBuffer
import User.TotpToken

case class TotpSecret(secret: Array[Byte]) extends AnyVal {
  import TotpSecret._

  override def toString = "TotpSecret(****)"

  def base32: String = new Base32().encodeAsString(secret)

  def currentTotp = totp(System.currentTimeMillis / 30000)

  def totp(period: Long): TotpToken =
    TotpToken {
      val msg = ByteBuffer.allocate(8).putLong(0, period).array

      val hmac = Mac.getInstance("HMACSHA1")
      hmac.init(new SecretKeySpec(secret, "RAW"))
      val hash = hmac.doFinal(msg)

      val offset = hash.last & 0xf
      otpString(ByteBuffer.wrap(hash).getInt(offset) & 0x7fffffff)
    }

  def verify(token: TotpToken): Boolean = {
    val period = System.currentTimeMillis / 30000
    skewList.exists(skew => totp(period + skew) == token)
  }
}

object TotpSecret {
  // clock skews in rough order of likelihood
  private val skewList = List(0, -1, 1, -2, 2, -3, 3)

  private def otpString(otp: Int) = {
    val s = (otp % 1000000).toString
    "0" * (6 - s.length) + s
  }

  private[this] val secureRandom = new SecureRandom()

  def apply(base32: String) = new TotpSecret(new Base32().decode(base32))

  def random: TotpSecret = {
    val secret = new Array[Byte](16)
    secureRandom.nextBytes(secret)
    TotpSecret(secret)
  }

  private[user] val totpSecretBSONHandler = lila.db.dsl.quickHandler[TotpSecret](
    { case v: BSONBinary => TotpSecret(v.byteArray) },
    v => BSONBinary(v.secret, Subtype.GenericBinarySubtype)
  )
}
