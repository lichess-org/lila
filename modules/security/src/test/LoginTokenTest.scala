package lila.security

class LoginTokenTest extends munit.FunSuite:

  private val query = Map(
    "email" -> Seq("old@lichess.org"),
    "username" -> Seq("olduser"),
    "code" -> Seq("234567")
  )

  test("body params take precedence over query params"):
    assertEquals(
      storedCodeParam("email", Map("email" -> Seq("new@lichess.org")), query),
      Some("new@lichess.org")
    )

  test("query params are used when the body is empty"):
    assertEquals(storedCodeParam("email", Map.empty, query), Some("old@lichess.org"))
    assertEquals(storedCodeParam("username", Map.empty, query), Some("olduser"))
    assertEquals(storedCodeParam("code", Map.empty, query), Some("234567"))

  test("empty body values fall back to the query string"):
    assertEquals(storedCodeParam("email", Map("email" -> Seq("")), query), Some("old@lichess.org"))
    assertEquals(storedCodeParam("email", Map("email" -> Seq("")), Map.empty), None)

  test("missing params yield none"):
    assertEquals(storedCodeParam("code", Map.empty, Map.empty), None)
