package lila.oidc

class LoginErrorTest extends munit.FunSuite:

  test("enabled account is never refused") {
    assert(
      LoginError.fromStatus(true, foreverClosed = false, deleted = false, globallyEnabled = None).isEmpty
    )
    assert(
      LoginError.fromStatus(true, foreverClosed = true, deleted = true, globallyEnabled = Some(true)).isEmpty
    )
  }

  test("global ban wins over local status") {
    assertEquals(
      LoginError.fromStatus(true, foreverClosed = false, deleted = false, globallyEnabled = Some(false)),
      Some(LoginError.Banned)
    )
    assertEquals(
      LoginError.fromStatus(false, foreverClosed = true, deleted = true, globallyEnabled = Some(false)),
      Some(LoginError.Banned)
    )
  }

  test("deleted tombstone wins over forever closed") {
    assertEquals(
      LoginError.fromStatus(false, foreverClosed = true, deleted = true, globallyEnabled = None),
      Some(LoginError.Deleted)
    )
    assertEquals(
      LoginError.fromStatus(false, foreverClosed = false, deleted = true, globallyEnabled = None),
      Some(LoginError.Deleted)
    )
  }

  test("forever closed") {
    assertEquals(
      LoginError.fromStatus(false, foreverClosed = true, deleted = false, globallyEnabled = None),
      Some(LoginError.ClosedForever)
    )
  }

  test("plain closure") {
    assertEquals(
      LoginError.fromStatus(false, foreverClosed = false, deleted = false, globallyEnabled = None),
      Some(LoginError.Closed)
    )
  }
