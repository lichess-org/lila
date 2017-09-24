package lila.user

import javax.crypto.Cipher
import javax.crypto.spec.{ IvParameterSpec, SecretKeySpec }
import java.util.Base64

/**
 * Encryption for bcrypt hashes.
 *
 *  Security is dependent on plaintext format.
 *  This class should not be used for other purposes.
 */
private[user] class DumbAes(secret: String) {
  // Bcrypt hashes start with a full block (16 bytes) of
  // random data, so a static IV won't break primitives.
  private val (sIV, sKey) = {
    val bs = Base64.getDecoder.decode(secret)
    if (bs.size != 32) throw new IllegalStateException
    (new IvParameterSpec(bs take 16), new SecretKeySpec(bs drop 16, "AES"))
  }

  @inline private def process(mode: Int, data: Array[Byte]) = {
    val c = Cipher.getInstance("AES/CTS/NoPadding")
    c.init(mode, sKey, sIV)
    c.doFinal(data)
  }

  import Cipher.{ ENCRYPT_MODE, DECRYPT_MODE }
  def encrypt(data: Array[Byte]) = process(ENCRYPT_MODE, data)
  def decrypt(data: Array[Byte]) = process(DECRYPT_MODE, data)
}

sealed class PasswordHasher(secret: String, logRounds: Int,
  hashTimer: (=> Array[Byte]) => Array[Byte] = x => x) {
  import org.mindrot.BCrypt

  private val aes = new DumbAes(secret)
  protected def bHash(pass: String, salt: Array[Byte]) =
    hashTimer(BCrypt.hashpwRaw(pass, 'a', logRounds, salt))

  def hash(pass: String) = {
    val salt = BCrypt.gensaltRaw
    aes.encrypt(salt ++ bHash(pass, salt))
  }

  def check(encHash: Array[Byte], pass: String) = encHash.size == 39 && {
    val (salt, hash) = aes.decrypt(encHash).splitAt(16)
    BCrypt.bytesEqualSecure(hash, bHash(pass, salt))
  }
}