package lila.user

import org.mindrot.BCrypt

import java.nio.charset.StandardCharsets.UTF_8

class BCryptTest extends munit.FunSuite:

  // From jBcrypt test suite.
  val pass = "abc"
  val b64Hash = "$2a$06$If6bvum7DFjUnE9p2uDeDu0YHzrHM6tf.iqN8.yx.jNN1ILEf7h0i"

  test("accept correct pass"):
    assert(BCrypt.checkpw(pass, b64Hash))
  test("reject bad password"):
    assert(!BCrypt.checkpw("", b64Hash))

  val salt = BCrypt.gensaltRaw
  test("have uniq salts"):
    assertNotEquals(salt, BCrypt.gensaltRaw)

  val rawHash = BCrypt.hashpwRaw(pass.getBytes(UTF_8), 'a', 6, salt)

  test("sizes"):
    assertEquals(salt.size, 16)
    assertEquals(rawHash.size, 23)

  import BCrypt.encode_base64 as bc64
  val bString = "$2a$06$" + bc64(salt) + bc64(rawHash)
  test("raw bytes accept good"):
    assert(BCrypt.checkpw(pass, bString))
  test("raw bytes reject bad"):
    assert(!BCrypt.checkpw("", bString))
