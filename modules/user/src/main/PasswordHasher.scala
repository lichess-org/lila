package lila.user

import javax.crypto.Cipher
import javax.crypto.spec.{ IvParameterSpec, SecretKeySpec }
import java.util.Base64

class PasswordHasher(secret: String) {
  private val (lameIv, sKey) = {
    val bs = Base64.getDecoder.decode(secret)
    if (bs.size != 32) throw new IllegalStateException
    (new IvParameterSpec(bs take 16), new SecretKeySpec(bs drop 16, "AES"))
  }

  // Static IV because bcrypt hashes start with 16 bytes of random data
  private def dumbAes(mode: Int, data: Array[Byte]) = {
    val c = Cipher.getInstance("AES/CTS/NoPadding")
    c.init(mode, sKey, lameIv)
    c.doFinal(data)
  }

  import org.mindrot.BCrypt
  import Cipher.{ ENCRYPT_MODE, DECRYPT_MODE }

  def hash(pass: String) = {
    val salt = BCrypt.gensaltRaw
    val hash = BCrypt.hashpwRaw(pass, 'a', 12, salt)
    dumbAes(ENCRYPT_MODE, salt ++ hash)
  }

  def check(encHash: Array[Byte], pass: String) = encHash.size == 39 && {
    val (salt, hash) = dumbAes(DECRYPT_MODE, encHash).splitAt(16)
    val newHash = BCrypt.hashpwRaw(pass, 'a', 12, salt)
    BCrypt.bytesEqualSecure(hash, newHash)
  }
}