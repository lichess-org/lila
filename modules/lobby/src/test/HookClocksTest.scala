package lila.lobby

import chess.{ Clock, Rated }

class HookClocksTest extends munit.FunSuite:

  val c3_0  = Clock.Config(180, 0)
  val c5_0  = Clock.Config(300, 0)
  val c5_3  = Clock.Config(300, 3)
  val c10_0 = Clock.Config(600, 0)

  def hook(clocks: NonEmptyList[Clock.Config]): Hook =
    Hook(
      id = "test",
      sri = lila.core.socket.Sri("sri"),
      sid = none,
      variant = chess.variant.Standard.id,
      clocks = clocks,
      rated = Rated.No,
      color = TriColor.Random,
      user = none,
      ratingRange = lila.core.rating.RatingRange.default,
      createdAt = java.time.Instant.now,
      boardApi = false
    )

  def one(c: Clock.Config)        = hook(NonEmptyList.one(c))
  def multi(cs: Clock.Config*)    = hook(NonEmptyList.fromListUnsafe(cs.toList))

  // --- single/single ---

  test("single/single: match when clock is identical"):
    assert(one(c5_0).compatibleWith(one(c5_0)))

  test("single/single: no match when clock differs"):
    assert(!one(c5_0).compatibleWith(one(c3_0)))

  // --- single/multi ---

  test("single/multi: match when the multi list includes that clock"):
    assert(one(c5_0).compatibleWith(multi(c3_0, c5_0, c10_0)))

  test("single/multi: no match when the multi list excludes that clock"):
    assert(!one(c5_3).compatibleWith(multi(c3_0, c5_0, c10_0)))

  // --- multi/multi ---

  test("multi/multi: match when lists share at least one clock"):
    assert(multi(c3_0, c5_0).compatibleWith(multi(c5_0, c10_0)))

  test("multi/multi: no match when lists are disjoint"):
    assert(!multi(c3_0, c5_0).compatibleWith(multi(c10_0)))

  // --- matchingClock ---

  test("matchingClock: returns the shortest clock from the intersection"):
    assertEquals(multi(c5_0, c10_0).matchingClock(multi(c3_0, c5_0, c10_0)), Some(c5_0))

  test("matchingClock: returns None when there is no overlap"):
    assertEquals(one(c3_0).matchingClock(one(c5_0)), None)

  test("matchingClock: single/single equal clocks returns that clock"):
    assertEquals(one(c5_0).matchingClock(one(c5_0)), Some(c5_0))
