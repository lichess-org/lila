package lila.user

import lila.common.config.Secret
import org.specs2.mutable.Specification
import User.{ ClearPassword => P }

class PasswordHasherTest extends Specification {

  "bad secrets throw exceptions" in {
    new Aes(Secret("")) must throwA[IllegalArgumentException]
    new PasswordHasher(Secret(""), 12) must throwA[IllegalArgumentException]
    new PasswordHasher(Secret("t="), 12) must throwA[IllegalArgumentException]
  }

  val secret = Secret(Array.fill(16)(1.toByte).toBase64)

  "aes" should {
    def emptyArr(i: Int) = new Array[Byte](i)

    val aes = new Aes(secret)
    val iv  = Aes.iv(emptyArr(16))

    "preserve size" in {
      aes.encrypt(iv, emptyArr(20)).size must_== 20
      aes.encrypt(iv, emptyArr(39)).size must_== 39
    }

    val plaintext = (1 to 20).map(_.toByte).toArray
    val encrypted = aes.encrypt(iv, plaintext)
    "encrypt input" >> { encrypted !== plaintext }
    "and decrypt" >> { aes.decrypt(iv, encrypted) === plaintext }
    "constant encryption" >> { encrypted === aes.encrypt(iv, plaintext) }

    val wrongIv = Aes.iv((1 to 16).map(_.toByte).toArray)
    "iv matters" >> { aes.decrypt(wrongIv, encrypted) !== plaintext }
  }

  "hasher" should {
    val passHasher = new PasswordHasher(secret, 2)
    val liHash     = passHasher.hash(P("abc"))
    "accept good" >> passHasher.check(liHash, P("abc"))
    "reject bad" >> !passHasher.check(liHash, P("abc "))
    "uniq hash" >> { liHash !== passHasher.hash(P("abc")) }
  }
}
