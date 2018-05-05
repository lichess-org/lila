package lila.user

import org.specs2.mutable.Specification

class TotpTest extends Specification {

  "totp" should {
    "read and write secret" in {
      val secret = TotpSecret.random
      TotpSecret(secret.base32).base32 must_== secret.base32
    }
  }
}
