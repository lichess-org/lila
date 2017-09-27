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

  @inline private def run(mode: Int, iv: Array[Byte], b: Array[Byte]) = {
    val c = Cipher.getInstance("AES/CTS/NoPadding")
    c.init(mode, sKey, new IvParameterSpec(iv))
    c.doFinal(b)
  }

  import Cipher.{ ENCRYPT_MODE, DECRYPT_MODE }
  def encrypt(iv: Array[Byte], b: Array[Byte]) = run(ENCRYPT_MODE, iv, b)
  def decrypt(iv: Array[Byte], b: Array[Byte]) = run(DECRYPT_MODE, iv, b)
}

final class PasswordHasher(secret: String, logRounds: Int,
    hashTimer: (=> Array[Byte]) => Array[Byte] = x => x) {
  import org.mindrot.BCrypt

  private val aes = new Aes(secret)
  private def bHash(salt: Array[Byte], pass: String) =
    hashTimer(BCrypt.hashpwRaw(pass.sha512, 'a', logRounds, salt))

  def hash(pass: String) = {
    val salt = new Array[Byte](16)
    new SecureRandom().nextBytes(salt)

    salt ++ aes.encrypt(salt, bHash(salt, pass))
  }

  def check(bytes: Array[Byte], pass: String) = bytes.size == 39 && {
    val (salt, encHash) = bytes.splitAt(16)
    val hash = aes.decrypt(salt, encHash)
    BCrypt.bytesEqualSecure(hash, bHash(salt, pass))
  }
}