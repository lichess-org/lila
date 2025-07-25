package lila.playban

import scalalib.model.Days

class PlaybanTest extends munit.FunSuite:

  import Outcome.*

  val userId = UserId("user")
  val brandNew = Days(scalalib.time.daysBetween(nowInstant.minusHours(1), nowInstant)) // 0
  val trusted = lila.core.security.UserTrust.Yes
  val untrusted = lila.core.security.UserTrust.No

  test("empty"):
    val rec = UserRecord(userId, none, none, none)
    assertEquals(rec.bannable(brandNew, trusted), None)

  test("new one abort"):
    val rec = UserRecord(userId, Vector(Abort).some, none, none)
    assertEquals(rec.bannable(brandNew, trusted), None)

  test("new 2 aborts"):
    val rec = UserRecord(userId, Vector.fill(2)(Abort).some, none, none)
    assert(rec.bannable(brandNew, trusted).isDefined)

  test("new 1 good and 2 aborts"):
    val rec = UserRecord(userId, Some(Good +: Vector.fill(2)(Abort)), none, none)
    assert(rec.bannable(brandNew, trusted).isDefined)

  test("new account abuse"):
    val outcomes = Vector(Abort, Good, Abort, Abort)
    val rec = UserRecord(userId, Some(outcomes), none, none)
    assert(rec.bannable(brandNew, trusted).isDefined)

  test("older account"):
    val outcomes = Vector(Abort, Good, Abort, Abort)
    val rec = UserRecord(userId, Some(outcomes), none, none)
    assert(rec.bannable(Days(1), trusted).isEmpty)

  test("good and aborts"):
    val outcomes = Vector(Good, Abort, Good, Abort, Abort)
    val rec = UserRecord(userId, outcomes.some, none, none)
    assert(rec.bannable(brandNew, trusted).isDefined)

  test("sandbag and aborts"):
    val outcomes = Vector(Sandbag, Sandbag, Abort, Sandbag, Abort, Abort)
    val rec = UserRecord(userId, outcomes.some, none, none)
    assert(rec.bannable(brandNew, trusted).isDefined)

  test("untrusted new single abort"):
    val rec = UserRecord(userId, Vector(Abort).some, none, none)
    assertEquals(rec.bannable(brandNew, untrusted), None)

  test("untrusted old many aborts"):
    val rec = UserRecord(userId, Vector(Abort, Abort).some, none, none)
    assert(rec.bannable(Days(7), untrusted).isDefined)
