package lila.playban

import scalalib.model.Days

class PlaybanTest extends munit.FunSuite:

  val userId   = UserId("user")
  val brandNew = Days(0)

  test("empty"):
    val rec = UserRecord(userId, none, none, none)
    assertEquals(rec.bannable(brandNew), None)

  test("new one abort"):
    val rec = UserRecord(userId, Vector(Outcome.Abort).some, none, none)
    assertEquals(rec.bannable(brandNew), None)

  test("new 2 aborts"):
    val rec = UserRecord(userId, Vector.fill(2)(Outcome.Abort).some, none, none)
    assertEquals(rec.bannable(brandNew), None)

  test("new 3 aborts"):
    val rec = UserRecord(userId, Vector.fill(3)(Outcome.Abort).some, none, none)
    assert(rec.bannable(brandNew).isDefined)

  test("good and aborts"):
    import Outcome.*
    val outcomes = Vector(Good, Abort, Good, Abort, Abort)
    val rec      = UserRecord(userId, outcomes.some, none, none)
    assert(rec.bannable(brandNew).isEmpty)

  test("sandbag and aborts"):
    import Outcome.*
    val outcomes = Vector(Sandbag, Sandbag, Abort, Sandbag, Abort, Abort)
    val rec      = UserRecord(userId, outcomes.some, none, none)
    assert(rec.bannable(brandNew).isDefined)
