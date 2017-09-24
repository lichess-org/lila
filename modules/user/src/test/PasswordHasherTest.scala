package lila.user

import org.specs2.mutable.Specification
import org.mindrot.BCrypt

class PasswordHasherTest extends Specification {

  "bad secrets throw exceptions" in {
    new DumbAes("") must throwA[IllegalStateException]
    new PasswordHasher("", 12) must throwA[IllegalStateException]
    new PasswordHasher("t=", 12) must throwA[IllegalArgumentException]
  }

  val secret = Array.fill(32)(1.toByte).toBase64
  
  "aes" should {
    val aes = new DumbAes(secret)
    def emptyArr(i: Int) = new Array[Byte](i)
    "preserve size" in {
      aes.encrypt(emptyArr(20)).size must_== 20
      aes.encrypt(emptyArr(39)).size must_== 39
    }

    val enc20 = aes.encrypt(emptyArr(20))
    "encrypt input" >> { enc20 !== emptyArr(20) }
    "and decrypt" >> { aes.decrypt(aes.encrypt(emptyArr(20))).sum == 0 }
    "constant encryption" >> { enc20 === aes.encrypt(emptyArr(20)) }
  }

  "hasher" should {
    val passHasher = new PasswordHasher(secret, 2)
    val liHash = passHasher.hash("abc")
    "accept good" >> passHasher.check(liHash, "abc")
    "reject bad" >> !passHasher.check(liHash, "abc ")
    "uniq hash" >> { liHash !== passHasher.hash("abc") }
  }
}