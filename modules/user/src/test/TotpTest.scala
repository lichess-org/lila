package lila.user

import java.nio.charset.StandardCharsets.UTF_8

import lila.core.user.TotpSecret
import lila.user.TotpSecret.*

class TotpTest extends munit.FunSuite:

  test("read and write secret"):
    val secret = random
    assertEquals(decode(secret.base32).base32, secret.base32)

  test("authenticate"):
    val secret = random
    val token = secret.currentTotp
    assert(secret.verify(token))

  test("not authenticate"):
    val secret = decode("base32secret3232")
    assert(!secret.verify(TotpToken("")))
    assert(!secret.verify(TotpToken("000000")))
    assert(!secret.verify(TotpToken("123456")))

  test("reference"):
    // https://tools.ietf.org/html/rfc6238#appendix-B
    val secret = new TotpSecret("12345678901234567890".getBytes(UTF_8))
    assertEquals(secret.totp(59 / 30), TotpToken("287082"))
    assertEquals(secret.totp(1111111109L / 30), TotpToken("081804"))
    assertEquals(secret.totp(1111111111L / 30), TotpToken("050471"))
    assertEquals(secret.totp(1234567890L / 30), TotpToken("005924"))
    assertEquals(secret.totp(2000000000L / 30), TotpToken("279037"))
    assertEquals(secret.totp(20000000000L / 30), TotpToken("353130"))
