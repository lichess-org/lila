package lila.security

import com.roundeights.hasher.Implicits.*
import scalalib.SecureRandom

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.{ IvParameterSpec, SecretKeySpec }

import lila.core.config.Secret
import lila.core.email.UserIdOrEmail
import lila.core.security.{ ClearPassword, HashedPassword }

/** Encryption for bcrypt hashes.
  *
  * CTS reveals input length, which is fine for this application.
  */
final private class Aes(secret: Secret):
  private val sKey =
    val sk = Base64.getDecoder.decode(secret.value)
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

final class PasswordHasher(
    secret: Secret,
    logRounds: Int,
    hashTimer: (=> Array[Byte]) => Array[Byte] = x => x
)(using Executor, lila.core.config.RateLimit):
  import org.mindrot.BCrypt

  private val aes = new Aes(secret)
  private def bHash(salt: Array[Byte], p: ClearPassword) =
    hashTimer(BCrypt.hashpwRaw(p.value.sha512, 'a', logRounds, salt))

  def hash(p: ClearPassword): HashedPassword =
    val salt = SecureRandom.nextBytes(16)
    HashedPassword(salt ++ aes.encrypt(Aes.iv(salt), bHash(salt, p)))

  def check(bytes: HashedPassword, p: ClearPassword): Boolean =
    parse(bytes).so: (salt, encHash) =>
      val hash = aes.decrypt(Aes.iv(salt), encHash)
      MessageDigest.isEqual(hash, bHash(salt, p))

  private def parse(h: HashedPassword) = Option.when(h.bytes.lengthIs == 39)(h.bytes.splitAt(16))

  import lila.core.net.IpAddress
  import lila.memo.RateLimit
  import play.api.mvc.RequestHeader
  import lila.core.config
  import lila.common.HTTPRequest

  private lazy val rateLimitPerIP = RateLimit[IpAddress](
    credits = 200,
    duration = 10.minutes,
    key = "password.hashes.ip"
  )

  private lazy val rateLimitPerUser = RateLimit[UserIdOrEmail](
    credits = 10,
    duration = 10.minutes,
    key = "password.hashes.user"
  )

  private lazy val rateLimitGlobal = RateLimit[String](
    credits = 12 * 10 * 60, // max out 12 cores for 60 seconds
    duration = 1.minute,
    key = "password.hashes.global"
  )

  def rateLimit[A](
      default: => Fu[A],
      enforce: config.RateLimit,
      ipCost: Int,
      userCost: Int = 1
  )(id: UserIdOrEmail, req: RequestHeader)(run: lila.memo.RateLimit.Charge => Fu[A]): Fu[A] =
    if enforce.yes then
      val ip = HTTPRequest.ipAddress(req)
      rateLimitPerUser.chargeable(id, default, cost = userCost, msg = s"IP: $ip"): chargeUser =>
        rateLimitPerIP.chargeable(ip, default, cost = ipCost): chargeIp =>
          rateLimitGlobal("-", default, msg = s"IP: $ip"):
            run: () =>
              chargeIp(ipCost)
              chargeUser(userCost)
    else run(() => ())
