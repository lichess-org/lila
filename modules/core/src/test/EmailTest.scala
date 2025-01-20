package lila.core

import lila.core.email.{ EmailAddress, NormalizedEmailAddress }

class EmailTest extends munit.FunSuite:

  test("normalize gmail"):
    assertEquals(
      EmailAddress("Hello.World+suffix1+suffix2@gmail.com").normalize,
      NormalizedEmailAddress("helloworld@gmail.com")
    )
    assertEquals(
      EmailAddress("foo.bar@googlemail.com").normalize,
      NormalizedEmailAddress("foobar@googlemail.com")
    )

  test("normalize other"):
    assertEquals(
      EmailAddress("Hello.World+suffix1+suffix2@yandex.ru").normalize,
      NormalizedEmailAddress("hello.world@yandex.ru")
    )
    assertEquals(
      EmailAddress("foo.bar@outlook.com").normalize,
      NormalizedEmailAddress("foo.bar@outlook.com")
    )

  test("not similar emails"):
    assert(!EmailAddress("test@mail.com").similarTo(EmailAddress("test@duck.com")))
    assert(!EmailAddress("test@mail.com").similarTo(EmailAddress("different@mail.com")))
    assert(!EmailAddress("test@yandex.com").similarTo(EmailAddress("different@ya.ru")))

  test("similar emails"):
    assert(EmailAddress("test@mail.com").similarTo(EmailAddress("test@mail.com")))
    assert(EmailAddress("test@gmail.com").similarTo(EmailAddress("test@googlemail.com")))
    assert(EmailAddress("test+foo@gmail.com").similarTo(EmailAddress("test@googlemail.com")))
    assert(EmailAddress("test@yandex.com").similarTo(EmailAddress("test@ya.ru")))

  test("lowercase emails"):
    assertEquals(
      EmailAddress("aBcDeFG@HOTMAIL.cOM").normalize,
      NormalizedEmailAddress("abcdefg@hotmail.com")
    )

  test("accept valid addresses"):
    assert(EmailAddress.from("Hello.World+suffix1+suffix2@gmail.com").isDefined)
    assert(EmailAddress.from("kebab-case@example.com").isDefined)
    assert(EmailAddress.from("snake_case@example.com").isDefined)
    assert(EmailAddress.from("_leading.underscore@example.com").isDefined)
    assert(EmailAddress.from("trailing.dash-@example.com").isDefined)

  test("reject invalid addresses"):
    assertEquals(EmailAddress.from(".leading.dot@example.com"), None)
    assertEquals(EmailAddress.from("trailing.dot.@example.com"), None)
    assertEquals(EmailAddress.from("underscore.in@domain_name.com"), None)
    assertEquals(EmailAddress.from("consecutive..dots@example.com"), None)
    assertEquals(EmailAddress.from("invalid<character@example.com"), None)
