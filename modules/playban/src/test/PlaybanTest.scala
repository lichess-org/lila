package lila.playban

import scalalib.model.Days

class PlaybanTest extends munit.FunSuite:

  import Outcome.*

  val userId   = UserId("user")
  val brandNew = Days(scalalib.time.daysBetween(nowInstant.minusHours(1), nowInstant)) // 0

  test("empty"):
    val rec = UserRecord(userId, none, none, none)
    assertEquals(rec.bannable(brandNew), None)

  test("new one abort"):
    val rec = UserRecord(userId, Vector(Abort).some, none, none)
    assertEquals(rec.bannable(brandNew), None)

  test("new 2 aborts"):
    val rec = UserRecord(userId, Vector.fill(2)(Abort).some, none, none)
    assert(rec.bannable(brandNew).isDefined)

  test("new 1 good and 2 aborts"):
    val rec = UserRecord(userId, Some(Good +: Vector.fill(2)(Abort)), none, none)
    assert(rec.bannable(brandNew).isDefined)

  test("new account abuse"):
    val outcomes = Vector(Abort, Good, Abort, Abort)
    val rec      = UserRecord(userId, Some(outcomes), none, none)
    assert(rec.bannable(brandNew).isDefined)

  test("older account"):
    val outcomes = Vector(Abort, Good, Abort, Abort)
    val rec      = UserRecord(userId, Some(outcomes), none, none)
    assert(rec.bannable(Days(1)).isEmpty)

  test("good and aborts"):
    val outcomes = Vector(Good, Abort, Good, Abort, Abort)
    val rec      = UserRecord(userId, outcomes.some, none, none)
    assert(rec.bannable(brandNew).isDefined)

  test("sandbag and aborts"):
    val outcomes = Vector(Sandbag, Sandbag, Abort, Sandbag, Abort, Abort)
    val rec      = UserRecord(userId, outcomes.some, none, none)
    assert(rec.bannable(brandNew).isDefined)
