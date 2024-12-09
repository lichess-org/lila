package lila.user

import org.apache.commons.codec.binary.Base32
import scalalib.SecureRandom

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import lila.core.user.TotpSecret

object TotpSecret:

  extension (it: TotpSecret)

    def base32: String = new Base32().encodeAsString(it.secret)

    def currentTotp = it.totp(System.currentTimeMillis / 30000)

    def totp(period: Long): TotpToken = TotpToken:
      val msg = ByteBuffer.allocate(8).putLong(0, period).array

      val hmac = Mac.getInstance("HMACSHA1")
      hmac.init(new SecretKeySpec(it.secret, "RAW"))
      val hash = hmac.doFinal(msg)

      val offset = hash.last & 0xf
      otpString(ByteBuffer.wrap(hash).getInt(offset) & 0x7fffffff)

    def verify(token: TotpToken): Boolean =
      val period = System.currentTimeMillis / 30000
      skewList.exists(skew => totp(period + skew) == token)

  // clock skews in rough order of likelihood
  private val skewList = List(0, -1, 1, -2, 2, -3, 3)

  private def otpString(otp: Int) =
    val s = (otp % 1000000).toString
    "0" * (6 - s.length) + s

  def decode(base32: String) = new TotpSecret(new Base32().decode(base32))

  def random = new TotpSecret(SecureRandom.nextBytes(20))
