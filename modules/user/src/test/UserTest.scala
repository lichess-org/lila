package lila.user

import lila.core.userId.{ UserSearch, UserStr }

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

  test("route ids are canonical only") {
    assertEquals(UserStr.read("1001").map(_.value), Some("1001"))
    assertEquals(UserStr.read("lichess").map(_.value), Some("lichess"))
    assertEquals(UserStr.read("!a1B2c3D4").map(_.value), Some("!a1B2c3D4"))
    assertEquals(UserStr.read("Александр"), None)
    assertEquals(UserStr.read("Иван Петров"), None)
    assertEquals(UserStr.read("thibault"), None)
  }

  test("user search is unicode aware and keeps inner spaces") {
    assertEquals(UserSearch.read("  Иван Петров ").map(_.value), Some("иван петров"))
    assertEquals(UserSearch.read("Александр").map(_.value), Some("александр"))
    assertEquals(UserSearch.read("@"), None)
  }

  test("username regex bad prefix: cannot signup") {
    assert(!canSignup("000"))
    // assert(!canSignup("0foo"))
    // assert(!canSignup("_foo"))
    // assert(!canSignup("__foo"))
    // assert(!canSignup("-foo"))
  }
