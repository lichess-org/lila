package lila.common

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.{ IvParameterSpec, SecretKeySpec }
import org.apache.commons.codec.binary.Hex
import scala.util.{ Failure, Try }

import lila.common.config.Secret

object SymmetricCipher {

  case class CipherFailedException(msg: String, cause: Throwable) extends Exception(msg, cause)
}

// based on https://synkre.com/scala-content-encryption/
final class SymmetricCipher(secret: Secret) {

  import SymmetricCipher._

  private val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
  private val random = new SecureRandom()
  private val key    = new SecretKeySpec("%16s".format(secret.value).getBytes, "AES")

  private object bytes {
    def encrypt(input: Array[Byte]): Try[Array[Byte]] =
      Try {
        cipher.init(Cipher.ENCRYPT_MODE, key, random)
        cipher.getIV ++ cipher.doFinal(input)
      } recoverWith { case e: Throwable =>
        e.printStackTrace()
        Failure(new CipherFailedException("Encryption failed", e))
      }

    def decrypt(input: Array[Byte]): Try[Array[Byte]] =
      Try {
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(input.slice(0, 16)))
        cipher.doFinal(input.slice(16, input.length))
      } recoverWith { case e: Throwable =>
        Failure(new CipherFailedException("Decryption failed", e))
      }
  }

  object base64 {
    def encrypt(text: String): Try[String] =
      bytes.encrypt(text.getBytes).map(Base64.getEncoder.encodeToString)

    def decrypt(encryptedBase64String: String): Try[String] =
      bytes.decrypt(Base64.getDecoder.decode(encryptedBase64String)).map(_.map(_.toChar).mkString)
  }

  object base64UrlFriendly {
    def encrypt(text: String): Try[String] =
      base64.encrypt(text).map(_.replace("/", "_"))

    def decrypt(encrypted: String): Try[String] =
      base64.decrypt(encrypted.replace("_", "/"))
  }

  object hex {
    def encrypt(s: String): Try[String] =
      bytes.encrypt(s.getBytes("UTF-8")).map(bytes => new String(Hex.encodeHex(bytes, false)))

    def decrypt(s: String): Try[String] =
      bytes.decrypt(Hex.decodeHex(s)).map(new String(_, "UTF-8"))
  }
}
