package lila.core

import lila.core.email.{ EmailAddress, NormalizedEmailAddress }

class EmailTest extends munit.FunSuite:

  test("normalize gmail") {
    assertEquals(
      EmailAddress("Hello.World+suffix1+suffix2@gmail.com").normalize,
      NormalizedEmailAddress("helloworld@gmail.com")
    )
    assertEquals(
      EmailAddress("foo.bar@googlemail.com").normalize,
      NormalizedEmailAddress("foobar@googlemail.com")
    )
  }
  test("lowercase emails") {
    assertEquals(
      EmailAddress("aBcDeFG@HOTMAIL.cOM").normalize,
      NormalizedEmailAddress("abcdefg@hotmail.com")
    )
  }

  test("accept valid addresses") {
    assert(EmailAddress.from("Hello.World+suffix1+suffix2@gmail.com").isDefined)
    assert(EmailAddress.from("kebab-case@example.com").isDefined)
    assert(EmailAddress.from("snake_case@example.com").isDefined)
    assert(EmailAddress.from("_leading.underscore@example.com").isDefined)
    assert(EmailAddress.from("trailing.dash-@example.com").isDefined)
  }
  test("reject invalid addresses") {
    assertEquals(EmailAddress.from(".leading.dot@example.com"), None)
    assertEquals(EmailAddress.from("trailing.dot.@example.com"), None)
    assertEquals(EmailAddress.from("underscore.in@domain_name.com"), None)
    assertEquals(EmailAddress.from("consecutive..dots@example.com"), None)
    assertEquals(EmailAddress.from("invalid<character@example.com"), None)
  }
