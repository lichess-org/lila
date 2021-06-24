package lila.user

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.{ IvParameterSpec, SecretKeySpec }
import com.roundeights.hasher.Implicits._

import lila.common.SecureRandom
import lila.common.config.Secret

/** Encryption for bcrypt hashes.
  *
  * CTS reveals input length, which is fine for
  * this application.
  */
final private class Aes(secret: Secret) {
  private val sKey = {
    val sk    = Base64.getDecoder.decode(secret.value)
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

  import Cipher.{ DECRYPT_MODE, ENCRYPT_MODE }
  def encrypt(iv: Aes.InitVector, b: Array[Byte]) = run(ENCRYPT_MODE, iv, b)
  def decrypt(iv: Aes.InitVector, b: Array[Byte]) = run(DECRYPT_MODE, iv, b)
}

private object Aes {
  type InitVector = IvParameterSpec

  def iv(bytes: Array[Byte]): InitVector = new IvParameterSpec(bytes)
}

case class HashedPassword(bytes: Array[Byte]) extends AnyVal {
  def parse = bytes.lengthIs == 39 option bytes.splitAt(16)
}

final private class PasswordHasher(
    secret: Secret,
    logRounds: Int,
    hashTimer: (=> Array[Byte]) => Array[Byte] = x => x
) {
  import org.mindrot.BCrypt
  import User.ClearPassword

  private val aes  = new Aes(secret)
  private def bHash(salt: Array[Byte], p: ClearPassword) =
    hashTimer(BCrypt.hashpwRaw(p.value.sha512, 'a', logRounds, salt))

  def hash(p: ClearPassword): HashedPassword = {
    val salt = new Array[Byte](16)
    SecureRandom.nextBytes(salt)
    HashedPassword(salt ++ aes.encrypt(Aes.iv(salt), bHash(salt, p)))
  }

  def check(bytes: HashedPassword, p: ClearPassword): Boolean =
    bytes.parse ?? { case (salt, encHash) =>
      val hash = aes.decrypt(Aes.iv(salt), encHash)
      BCrypt.bytesEqualSecure(hash, bHash(salt, p))
    }
}

object PasswordHasher {

  import scala.concurrent.duration._
  import play.api.mvc.RequestHeader
  import lila.memo.RateLimit
  import lila.common.{ HTTPRequest, IpAddress }

  private lazy val rateLimitPerIP = new RateLimit[IpAddress](
    credits = 40 * 2, // double cost in case of hash check failure
    duration = 8 minutes,
    key = "password.hashes.ip"
  )

  private lazy val rateLimitPerUser = new RateLimit[String](
    credits = 10,
    duration = 30 minutes,
    key = "password.hashes.user"
  )

  private lazy val rateLimitGlobal = new RateLimit[String](
    credits = 4 * 10 * 60, // max out 4 cores for 60 seconds
    duration = 1 minute,
    key = "password.hashes.global"
  )

  def rateLimit[A](
      enforce: lila.common.config.RateLimit
  )(username: String, req: RequestHeader)(run: RateLimit.Charge => Fu[A])(default: => Fu[A]): Fu[A] =
    if (enforce.value) {
      val cost = 1
      val ip   = HTTPRequest ipAddress req
      rateLimitPerUser(User normalize username, cost = cost) {
        rateLimitPerIP.chargeable(ip, cost = cost) { charge =>
          rateLimitGlobal("-", cost = cost, msg = ip.value) {
            run(charge)
          }(default)
        }(default)
      }(default)
    } else run(_ => ())
}
