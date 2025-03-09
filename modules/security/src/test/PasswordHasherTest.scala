package lila.security

import lila.core.config.Secret
import lila.core.security.ClearPassword as P

class PasswordHasherTest extends munit.FunSuite:

  given Executor                             = scala.concurrent.ExecutionContextOpportunistic
  given lila.core.config.RateLimit           = lila.core.config.RateLimit.No
  extension (self: Array[Byte]) def toBase64 = java.util.Base64.getEncoder.encodeToString(self)

  test("bad secrets throw exceptions"):
    intercept[IllegalArgumentException]:
      new Aes(Secret(""))
    intercept[IllegalArgumentException]:
      new PasswordHasher(Secret(""), 12)
    intercept[IllegalArgumentException]:
      new PasswordHasher(Secret("t="), 12)

  val secret = Secret(Array.fill(16)(1.toByte).toBase64)

  def emptyArr(i: Int) = new Array[Byte](i)

  val aes = new Aes(secret)
  val iv  = Aes.iv(emptyArr(16))

  test("aes preserve size"):
    assertEquals(aes.encrypt(iv, emptyArr(20)).size, 20)
    assertEquals(aes.encrypt(iv, emptyArr(39)).size, 39)

  val plaintext = (1 to 20).map(_.toByte).toArray
  val encrypted = aes.encrypt(iv, plaintext)
  test("aes encrypt input"):
    assertNotEquals(encrypted, plaintext)
  test("aes and decrypt"):
    assertEquals(aes.decrypt(iv, encrypted).toSeq, plaintext.toSeq)
  test("aes constant encryption"):
    assertEquals(encrypted.toSeq, aes.encrypt(iv, plaintext).toSeq)

  val wrongIv = Aes.iv((1 to 16).map(_.toByte).toArray)
  test("aes iv matters"):
    assertNotEquals(aes.decrypt(wrongIv, encrypted), plaintext)

  val passHasher = new PasswordHasher(secret, 2)
  val liHash     = passHasher.hash(P("abc"))
  test("hasher accept good"):
    assert(passHasher.check(liHash, P("abc")))
  test("hasher reject bad"):
    assert(!passHasher.check(liHash, P("abc ")))
  test("hasher uniq hash"):
    assertNotEquals(liHash, passHasher.hash(P("abc")))
