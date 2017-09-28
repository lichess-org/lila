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
private[user] final class Aes(secret: String) {
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

private[user] object Aes {
  type InitVector = IvParameterSpec

  def iv(bytes: Array[Byte]): InitVector = new IvParameterSpec(bytes)
}

case class HashedPassword(bytes: Array[Byte]) extends AnyVal {
  def parse = bytes.length == 39 option bytes.splitAt(16)
}

final class PasswordHasher(
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
