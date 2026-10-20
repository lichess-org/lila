package lila.user

class UserTest extends munit.FunSuite:

  given Conversion[String, UserId] = UserId(_)

  def canSignup(str: String) =
    import lila.user.nameRules.*
    newUsernameRegex.pattern.matcher(str).matches &&
    newUsernamePrefix.pattern.matcher(str).matches &&
    newUsernameSuffix.pattern.matcher(str).matches &&
    newUsernameChars.pattern.matcher(str).matches &&
    newUsernameLetters.pattern.matcher(str).matches

  def canRoute(str: String) = UserStr(str).couldBeUserId

  test("route ids accept numeric and system ids only") {
    assert(canRoute("2"))
    assert(canRoute("1001"))
    assert(canRoute("lichess"))
    assert(canRoute("ghost"))
    assert(canRoute("!a1B2c3D4"))
    assert(!canRoute("Александр"))
    assert(!canRoute("Иван Петров"))
    assert(!canRoute("thibault"))
    assert(!canRoute("0foo"))
    assert(!canRoute(""))
  }

  test("username regex bad prefix: cannot signup") {
    assert(!canSignup("000"))
    // assert(!canSignup("0foo"))
    // assert(!canSignup("_foo"))
    // assert(!canSignup("__foo"))
    // assert(!canSignup("-foo"))
  }
