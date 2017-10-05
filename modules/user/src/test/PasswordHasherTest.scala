package lila.user

import org.specs2.mutable.Specification
import org.mindrot.BCrypt
import User.{ ClearPassword => P }

class PasswordHasherTest extends Specification {

  "bad secrets throw exceptions" in {
    new Aes("") must throwA[IllegalArgumentException]
    new PasswordHasher("", 12) must throwA[IllegalArgumentException]
    new PasswordHasher("t=", 12) must throwA[IllegalArgumentException]
  }

  val secret = Array.fill(16)(1.toByte).toBase64

  "aes" should {
    def emptyArr(i: Int) = new Array[Byte](i)

    val aes = new Aes(secret)
    val iv = Aes.iv(emptyArr(16))

    "preserve size" in {
      aes.encrypt(iv, emptyArr(20)).size must_== 20
      aes.encrypt(iv, emptyArr(39)).size must_== 39
    }

    val enc20 = aes.encrypt(iv, emptyArr(20))
    "encrypt input" >> { enc20 !== emptyArr(20) }
    "and decrypt" >> { aes.decrypt(iv, aes.encrypt(iv, emptyArr(20))).sum == 0 }
    "constant encryption" >> { enc20 === aes.encrypt(iv, emptyArr(20)) }
  }

  "hasher" should {
    val passHasher = new PasswordHasher(secret, 2)
    val liHash = passHasher.hash(P("abc"))
    "accept good" >> passHasher.check(liHash, P("abc"))
    "reject bad" >> !passHasher.check(liHash, P("abc "))
    "uniq hash" >> { liHash !== passHasher.hash(P("abc")) }
  }
}
