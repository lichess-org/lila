package lila.user

import java.nio.charset.StandardCharsets.UTF_8

import org.specs2.mutable.Specification
import org.mindrot.BCrypt

class BCryptTest extends Specification {

  // From jBcrypt test suite.
  val pass    = "abc"
  val b64Hash = "$2a$06$If6bvum7DFjUnE9p2uDeDu0YHzrHM6tf.iqN8.yx.jNN1ILEf7h0i"

  "bcrypt" should {
    "accept correct pass" >> BCrypt.checkpw(pass, b64Hash)
    "reject bad password" >> !BCrypt.checkpw("", b64Hash)

    val salt = BCrypt.gensaltRaw
    "have uniq salts" >> { salt !== BCrypt.gensaltRaw }

    "raw bytes" in {
      val rawHash = BCrypt.hashpwRaw(pass.getBytes(UTF_8), 'a', 6, salt)

      salt.size must_== 16
      rawHash.size must_== 23

      import BCrypt.{ encode_base64 => bc64 }
      val bString = "$2a$06$" + bc64(salt) + bc64(rawHash)
      "accept good" >> BCrypt.checkpw(pass, bString)
      "reject bad" >> !BCrypt.checkpw("", bString)
    }
  }
}
