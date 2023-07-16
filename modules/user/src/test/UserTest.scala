package lila.user

class UserTest extends munit.FunSuite:

  given Conversion[String, UserStr] = UserStr(_)
  given Conversion[String, UserId]  = UserId(_)

  def canSignup(str: String) =
    User.newUsernamePrefix.pattern.matcher(str).matches && User.newUsernameSuffix.pattern
      .matcher(str)
      .matches &&
      User.newUsernameChars.pattern.matcher(str).matches &&
      User.newUsernameLetters.pattern.matcher(str).matches

  import User.couldBeUsername

  test("username regex bad prefix: can login") {
    assert(couldBeUsername("000"))
    assert(couldBeUsername("0foo"))
    assert(!couldBeUsername("_foo"))
    assert(!couldBeUsername("-foo"))
  }

  test("username regex bad prefix: cannot signup") {
    assert(!canSignup("000"))
    assert(!canSignup("0foo"))
    assert(!canSignup("_foo"))
    assert(!canSignup("-foo"))
  }

  test("username regex bad suffix") {
    assert(!couldBeUsername("a_"))
    assert(!couldBeUsername("a_"))
  }

  test("username regex too many consecutive non-letter chars") {
    assert(!canSignup("a_-a"))
    assert(!canSignup("_-a"))
    assert(!canSignup("a__a"))
    assert(!canSignup("a_-a"))
    assert(!canSignup("a--a"))
    assert(!canSignup("a--_"))
    assert(couldBeUsername("a--a"))
    assert(canSignup("a-a"))
    assert(canSignup("a_a"))
    assert(canSignup("a333"))
    assert(canSignup("ksean222"))
    assert(canSignup("Ksean222"))
  }

  test("username regex OK things") {
    assert(couldBeUsername("g-foo"))
    assert(couldBeUsername("G_FOo"))
    assert(couldBeUsername("g-foo"))
  }
