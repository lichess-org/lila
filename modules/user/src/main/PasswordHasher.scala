package lila.user

import javax.crypto.Cipher
import javax.crypto.spec.{ IvParameterSpec, SecretKeySpec }
import java.util.Base64

class PasswordHasher(secret: String) {
  private val sKey = {
    val decoded = Base64.getDecoder.decode(secret)
    if (decoded.size != 32) throw new AssertionError
    new SecretKeySpec(decoded, "AES")
  }

  private def aes(mode: Int, iv: Array[Byte], data: Array[Byte]) = {
    if (iv.size != 16) throw new AssertionError
    val c = Cipher.getInstance("AES/CTS/NoPadding")
    c.init(mode, sKey, new IvParameterSpec(iv))
    c.doFinal(data)
  }

  import org.mindrot.BCrypt
  import Cipher.{ ENCRYPT_MODE, DECRYPT_MODE }

  def hash(pass: String) = {
    val salt = BCrypt.gensaltRaw
    val hash = BCrypt.hashpwRaw(pass, 'a', 12, salt)

    salt ++ aes(ENCRYPT_MODE, salt, hash)
  }

  def check(pass: String, storedData: Array[Byte]) = {
    val (salt, encHash) = storedData.splitAt(16)
    val newHash = BCrypt.hashpwRaw(pass, 'a', 12, salt)

    val storedHash = aes(DECRYPT_MODE, salt, encHash)

    BCrypt.bytesEqualSecure(storedHash, newHash)
  }
}