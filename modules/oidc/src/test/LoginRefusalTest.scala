package lila.oidc

class LoginRefusalTest extends munit.FunSuite:

  test("enabled account is never refused") {
    assert(LoginRefusal.of(enabled = true, foreverClosed = false, deleted = false).isEmpty)
    assert(LoginRefusal.of(enabled = true, foreverClosed = true, deleted = true).isEmpty)
  }

  test("deleted tombstone wins over forever closed") {
    assertEquals(LoginRefusal.of(false, true, true), Some(LoginRefusal.Deleted))
    assertEquals(LoginRefusal.of(false, false, true), Some(LoginRefusal.Deleted))
  }

  test("forever closed") {
    assertEquals(LoginRefusal.of(false, true, false), Some(LoginRefusal.ClosedForever))
  }

  test("plain closure") {
    assertEquals(LoginRefusal.of(false, false, false), Some(LoginRefusal.Closed))
  }
