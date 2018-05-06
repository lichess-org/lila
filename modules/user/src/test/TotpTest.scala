package lila.user

import org.specs2.mutable.Specification
import User.TotpToken

class TotpTest extends Specification {

  "totp" should {
    "read and write secret" in {
      val secret = TotpSecret.random
      TotpSecret(secret.base32).base32 must_== secret.base32
    }

    "authenticate" in {
      val secret = TotpSecret.random
      val token = secret.currentTotp
      secret.verify(token) must beTrue
    }

    "not authenticate" in {
      val secret = TotpSecret("1234567890123456")
      secret.verify(TotpToken("")) must beFalse
      secret.verify(TotpToken("000000")) must beFalse
      secret.verify(TotpToken("123456")) must beFalse
    }
  }
}
