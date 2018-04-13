package lila.user

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.{ IvParameterSpec, SecretKeySpec }

import com.roundeights.hasher.Implicits._

/**
 * Encryption for bcrypt hashes.
 *
 * CTS reveals input length, which is fine for
 * this application.
 */
private final class Aes(secret: String) {
  private val sKey = {
    val sk = Base64.getDecoder.decode(secret)
    val kBits = sk.length * 8
    if (kBits != 128) {
      if (!(kBits == 192 || kBits == 256)) throw new IllegalArgumentException
      if (kBits > Cipher.getMaxAllowedKeyLength("AES/CTS/NoPadding"))
        throw new IllegalStateException(s"$kBits bit AES unavailable")
    }
    new SecretKeySpec(sk, "AES")
  }

  @inline private def run(mode: Int, iv: Aes.InitVector, b: Array[Byte]) = {
    val c = Cipher.getInstance("AES/CTS/NoPadding")
    c.init(mode, sKey, iv)
    c.doFinal(b)
  }

  import Cipher.{ ENCRYPT_MODE, DECRYPT_MODE }
  def encrypt(iv: Aes.InitVector, b: Array[Byte]) = run(ENCRYPT_MODE, iv, b)
  def decrypt(iv: Aes.InitVector, b: Array[Byte]) = run(DECRYPT_MODE, iv, b)
}

private object Aes {
  type InitVector = IvParameterSpec

  def iv(bytes: Array[Byte]): InitVector = new IvParameterSpec(bytes)
}

case class HashedPassword(bytes: Array[Byte]) extends AnyVal {
  def parse = bytes.length == 39 option bytes.splitAt(16)
}

private final class PasswordHasher(
    secret: String,
    logRounds: Int,
    hashTimer: (=> Array[Byte]) => Array[Byte] = x => x
) {
  import org.mindrot.BCrypt
  import User.ClearPassword

  private val aes = new Aes(secret)
  private def bHash(salt: Array[Byte], p: ClearPassword) =
    hashTimer(BCrypt.hashpwRaw(p.value.sha512, 'a', logRounds, salt))

  def hash(p: ClearPassword): HashedPassword = {
    val salt = new Array[Byte](16)
    new SecureRandom().nextBytes(salt)

    HashedPassword(salt ++ aes.encrypt(Aes.iv(salt), bHash(salt, p)))
  }

  def check(bytes: HashedPassword, p: ClearPassword): Boolean = bytes.parse ?? {
    case (salt, encHash) =>
      val hash = aes.decrypt(Aes.iv(salt), encHash)
      BCrypt.bytesEqualSecure(hash, bHash(salt, p))
  }
}

object PasswordHasher {

  import scala.concurrent.duration._
  import play.api.mvc.RequestHeader
  import ornicar.scalalib.Zero
  import lila.memo.RateLimit
  import lila.common.{ IpAddress, HTTPRequest }

  private lazy val rateLimitPerIP = new RateLimit[IpAddress](
    credits = 20 * 2, // double cost in case of hash check failure
    duration = 10 minutes,
    name = "Password hashes per IP",
    key = "password.hashes.ip"
  )

  private lazy val rateLimitPerUA = new RateLimit[String](
    credits = 30,
    duration = 20 seconds,
    name = "Password hashes per UA",
    key = "password.hashes.ua"
  )

  private lazy val rateLimitPerUser = new RateLimit[String](
    credits = 10,
    duration = 1.hour,
    name = "Password hashes per user",
    key = "password.hashes.user"
  )

  private lazy val rateLimitGlobal = new RateLimit[String](
    credits = 4 * 10 * 60, // max out 4 cores for 60 seconds
    duration = 1 minute,
    name = "Password hashes global",
    key = "password.hashes.global"
  )

  def rateLimit[A: Zero](username: String, req: RequestHeader)(run: RateLimit.Charge => Fu[A]): Fu[A] = {
    val cost = 1
    val ip = HTTPRequest lastRemoteAddress req
    rateLimitPerUser(username, cost = cost) {
      rateLimitPerIP.chargeable(ip, cost = cost) { charge =>
        rateLimitPerUA(~HTTPRequest.userAgent(req), cost = cost, msg = ip.value) {
          rateLimitGlobal("-", cost = cost, msg = ip.value) {
            run(charge)
          }
        }
      }
    }
  }
}
