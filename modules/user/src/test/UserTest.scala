package lila.user

class UserTest extends munit.FunSuite:

  given Conversion[String, UserStr] = UserStr(_)
  given Conversion[String, UserId] = UserId(_)

  def canSignup(str: String) =
    import lila.user.nameRules.*
    newUsernameRegex.pattern.matcher(str).matches &&
    newUsernamePrefix.pattern.matcher(str).matches &&
    newUsernameSuffix.pattern.matcher(str).matches &&
    newUsernameChars.pattern.matcher(str).matches &&
    newUsernameLetters.pattern.matcher(str).matches

  import UserStr.couldBeUsername

  test("username regex bad prefix: can login"):
    assert(couldBeUsername("000"))
    assert(couldBeUsername("0foo"))
    assert(couldBeUsername("_foo"))
    assert(couldBeUsername("__foo"))
    assert(couldBeUsername("-foo"))

  test("username regex bad prefix: cannot signup"):
    assert(!canSignup("000"))
    assert(!canSignup("0foo"))
    assert(!canSignup("_foo"))
    assert(!canSignup("__foo"))
    assert(!canSignup("-foo"))

  test("username regex bad suffix: can login"):
    assert(couldBeUsername("a_"))
    assert(couldBeUsername("a-"))

  test("username regex bad suffix: cannot signup"):
    assert(!canSignup("a_"))
    assert(!canSignup("a-"))

  test("username regex bad length: cannot login"):
    assert(!couldBeUsername(""))
    assert(!couldBeUsername("a"))
    assert(!couldBeUsername("A123456789012345678901234567890"))
    assert(!couldBeUsername("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))

  test("username regex bad length: cannot signup"):
    assert(!canSignup(""))
    assert(!canSignup("a"))
    assert(!canSignup("A123456789012345678901234567890"))
    assert(!canSignup("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))

  test("username regex too many consecutive non-letter chars"):
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

  test("username regex ok names: can login"):
    assert(couldBeUsername("g-foo"))
    assert(couldBeUsername("G_FOo"))
    assert(couldBeUsername("g-foO"))
    assert(couldBeUsername("FOOO"))
    assert(couldBeUsername("AB"))
    assert(couldBeUsername("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
    assert(couldBeUsername("A12345678901234567890123456789"))

  test("username regex ok names: can signup"):
    assert(canSignup("g-foo"))
    assert(canSignup("G_FOo"))
    assert(canSignup("g-foO"))
    assert(canSignup("FOOO"))
    assert(canSignup("AB"))
    assert(canSignup("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
    assert(canSignup("A12345678901234567890123456789"))
