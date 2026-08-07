package lila.security

class EmailTest extends munit.FunSuite:

  test("not similar emails"):
    assert(!EmailAddress("test@mail.com").similarTo(EmailAddress("test@duck.com")))
    assert(!EmailAddress("test@mail.com").similarTo(EmailAddress("different@mail.com")))
    assert(!EmailAddress("test@yandex.com").similarTo(EmailAddress("different@ya.ru")))

  test("similar emails"):
    assert(EmailAddress("test@mail.com").similarTo(EmailAddress("test@mail.com")))
    assert(EmailAddress("test@gmail.com").similarTo(EmailAddress("test@googlemail.com")))
    assert(EmailAddress("test+foo@gmail.com").similarTo(EmailAddress("test@googlemail.com")))
    assert(EmailAddress("test@yandex.com").similarTo(EmailAddress("test@ya.ru")))
