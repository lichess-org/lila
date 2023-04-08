package lila.user

import java.nio.charset.StandardCharsets.UTF_8
import org.specs2.mutable.Specification
import User.TotpToken

class TotpTest extends Specification {

  "totp" >> {
    "read and write secret" >> {
      val secret = TotpSecret.random
      TotpSecret(secret.base32).base32 === secret.base32
    }

    "authenticate" >> {
      val secret = TotpSecret.random
      val token  = secret.currentTotp
      secret.verify(token) must beTrue
    }

    "not authenticate" >> {
      val secret = TotpSecret("base32secret3232")
      secret.verify(TotpToken("")) must beFalse
      secret.verify(TotpToken("000000")) must beFalse
      secret.verify(TotpToken("123456")) must beFalse
    }

    "reference" >> {
      // https://tools.ietf.org/html/rfc6238#appendix-B
      val secret = TotpSecret("12345678901234567890".getBytes(UTF_8))
      secret.totp(59 / 30) === TotpToken("287082")
      secret.totp(1111111109L / 30) === TotpToken("081804")
      secret.totp(1111111111L / 30) === TotpToken("050471")
      secret.totp(1234567890L / 30) === TotpToken("005924")
      secret.totp(2000000000L / 30) === TotpToken("279037")
      secret.totp(20000000000L / 30) === TotpToken("353130")
    }
  }
}
