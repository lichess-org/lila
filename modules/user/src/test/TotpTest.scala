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
      val token  = secret.currentTotp
      secret.verify(token) must beTrue
    }

    "not authenticate" in {
      val secret = TotpSecret("base32secret3232")
      secret.verify(TotpToken("")) must beFalse
      secret.verify(TotpToken("000000")) must beFalse
      secret.verify(TotpToken("123456")) must beFalse
    }

    "reference" in {
      // https://tools.ietf.org/html/rfc6238#appendix-B
      val secret = TotpSecret("12345678901234567890".getBytes)
      secret.totp(59 / 30) must_== TotpToken("287082")
      secret.totp(1111111109L / 30) must_== TotpToken("081804")
      secret.totp(1111111111L / 30) must_== TotpToken("050471")
      secret.totp(1234567890L / 30) must_== TotpToken("005924")
      secret.totp(2000000000L / 30) must_== TotpToken("279037")
      secret.totp(20000000000L / 30) must_== TotpToken("353130")
    }
  }
}
