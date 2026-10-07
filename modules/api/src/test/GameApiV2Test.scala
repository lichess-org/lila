package lila.api

import java.time.Instant
import GameApiV2.inCreatedAtRange

class GameApiV2Test extends munit.FunSuite:

  // https://github.com/lichess-org/lila/issues/15929
  private val gameCreatedAt = Instant.ofEpochMilli(1690242797625L)
  private val since = Instant.ofEpochMilli(1690242797626L) // 1ms later, same epoch second
  private val until = Instant.ofEpochMilli(1700000000000L)

  test("no bounds include every game") {
    assert(inCreatedAtRange(gameCreatedAt, none, none))
  }

  test("game created 1ms before since is excluded") {
    assert(!inCreatedAtRange(gameCreatedAt, since.some, none))
    assert(!inCreatedAtRange(gameCreatedAt, since.some, until.some))
  }

  test("game created at since is included") {
    assert(inCreatedAtRange(since, since.some, none))
  }

  test("game created after since is included") {
    assert(inCreatedAtRange(since.plusMillis(1), since.some, none))
  }

  test("game created before until is included") {
    assert(inCreatedAtRange(until.minusMillis(1), none, until.some))
  }

  test("game created at until is excluded") {
    assert(!inCreatedAtRange(until, none, until.some))
  }

  test("game created inside both bounds is included") {
    assert(inCreatedAtRange(until.minusMillis(1), since.some, until.some))
  }
