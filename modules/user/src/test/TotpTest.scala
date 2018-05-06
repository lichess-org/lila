package lila.user

import org.specs2.mutable.Specification

class TotpTest extends Specification {

  "totp" should {
    "read and write secret" in {
      val secret = TotpSecret.random
      TotpSecret(secret.base32).base32 must_== secret.base32
    }

    "authenticate" in {
      val secret = TotpSecret.random
      val token = secret.totp(System.currentTimeMillis / 30000)
      secret.verify(token) must beTrue
    }

    "not authenticate" in {
      val secret = TotpSecret("1234567890123456")
      secret.verify("") must beFalse
      secret.verify("000000") must beFalse
      secret.verify("123456") must beFalse
    }
  }
}
