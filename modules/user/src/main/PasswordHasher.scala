package lila.user

import java.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.{ IvParameterSpec, SecretKeySpec }
import com.roundeights.hasher.Implicits.*
import ornicar.scalalib.SecureRandom

import lila.common.config.Secret

/** Encryption for bcrypt hashes.
  *
  * CTS reveals input length, which is fine for this application.
  */
final private class Aes(secret: Secret):
  private val sKey =
    val sk    = Base64.getDecoder.decode(secret.value)
    val kBits = sk.length * 8
    if kBits != 128 then
      if !(kBits == 192 || kBits == 256) then throw new IllegalArgumentException
      if kBits > Cipher.getMaxAllowedKeyLength("AES/CTS/NoPadding") then
        throw new IllegalStateException(s"$kBits bit AES unavailable")
    new SecretKeySpec(sk, "AES")

  private def run(mode: Int, iv: Aes.InitVector, b: Array[Byte]) =
    val c = Cipher.getInstance("AES/CTS/NoPadding")
    c.init(mode, sKey, iv)
    c.doFinal(b)

  import Cipher.{ DECRYPT_MODE, ENCRYPT_MODE }
  def encrypt(iv: Aes.InitVector, b: Array[Byte]) = run(ENCRYPT_MODE, iv, b)
  def decrypt(iv: Aes.InitVector, b: Array[Byte]) = run(DECRYPT_MODE, iv, b)

private object Aes:
  type InitVector = IvParameterSpec

  def iv(bytes: Array[Byte]): InitVector = new IvParameterSpec(bytes)

case class HashedPassword(bytes: Array[Byte]) extends AnyVal:
  def parse     = bytes.lengthIs == 39 option bytes.splitAt(16)
  def isBlanked = bytes.isEmpty

final private class PasswordHasher(
    secret: Secret,
    logRounds: Int,
    hashTimer: (=> Array[Byte]) => Array[Byte] = x => x
):
  import org.mindrot.BCrypt
  import User.ClearPassword

  private val aes = new Aes(secret)
  private def bHash(salt: Array[Byte], p: ClearPassword) =
    hashTimer(BCrypt.hashpwRaw(p.value.sha512, 'a', logRounds, salt))

  def hash(p: ClearPassword): HashedPassword =
    val salt = SecureRandom.nextBytes(16)
    HashedPassword(salt ++ aes.encrypt(Aes.iv(salt), bHash(salt, p)))

  def check(bytes: HashedPassword, p: ClearPassword): Boolean =
    bytes.parse.so: (salt, encHash) =>
      val hash = aes.decrypt(Aes.iv(salt), encHash)
      MessageDigest.isEqual(hash, bHash(salt, p))

object PasswordHasher:

  import play.api.mvc.RequestHeader
  import lila.memo.RateLimit
  import lila.common.{ HTTPRequest, IpAddress }

  private lazy val rateLimitPerIP = RateLimit[IpAddress](
    credits = 200,
    duration = 10 minutes,
    key = "password.hashes.ip"
  )

  private lazy val rateLimitPerUser = RateLimit[UserIdOrEmail](
    credits = 10,
    duration = 10 minutes,
    key = "password.hashes.user"
  )

  private lazy val rateLimitGlobal = RateLimit[String](
    credits = 12 * 10 * 60, // max out 12 cores for 60 seconds
    duration = 1 minute,
    key = "password.hashes.global"
  )

  def rateLimit[A](
      default: => Fu[A],
      enforce: lila.common.config.RateLimit,
      ipCost: Int,
      userCost: Int = 1
  )(id: UserIdOrEmail, req: RequestHeader)(run: RateLimit.Charge => Fu[A]): Fu[A] =
    if enforce.yes then
      val ip = HTTPRequest ipAddress req
      rateLimitPerUser.chargeable(id, default, cost = userCost, msg = s"IP: $ip"): chargeUser =>
        rateLimitPerIP.chargeable(ip, default, cost = ipCost): chargeIp =>
          rateLimitGlobal("-", default, msg = s"IP: $ip"):
            run: () =>
              chargeIp(ipCost)
              chargeUser(userCost)
    else run(() => ())
