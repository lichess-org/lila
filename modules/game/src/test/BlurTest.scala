package lila.game

import lila.core.game.Blurs
import lila.game.Blurs.{ *, given }
import lila.db.dsl.*
import reactivemongo.api.bson.*

class BlurTest extends munit.FunSuite:

  def fromMoveIndexes(indexes: Iterable[Int]): Blurs =
    indexes.foldLeft(Blurs(0L)): (b, i) =>
      b.addAtMoveIndex(i)

  test("addAtMoveIndex"):
    assertEquals(fromMoveIndexes(List(1, 2, 3, 6, 8)).binaryString, "011100101")

  val lilBlur = fromMoveIndexes(0 until 4)
  val bigBlur = fromMoveIndexes(0 until 40)

  test("BSON handler, lil blur"):
    val bson = lila.game.Blurs.blursHandler.writeOpt(lilBlur).get
    assertEquals(bson, BSONInteger(15))
    assertEquals(lilBlur, lila.game.Blurs.blursHandler.readOpt(bson).get)

  test("BSON handler, big blur"):
    val bson = lila.game.Blurs.blursHandler.writeOpt(bigBlur).get
    assertEquals(bson, BSONLong(1099511627775L))
    assertEquals(bigBlur, lila.game.Blurs.blursHandler.readOpt(bson).get)

  test("read int using blursHandler"):
    val bson = BSONInteger(-285641920)
    val read = lila.game.Blurs.blursHandler.readOpt(bson).get
    assertEquals(read.nb, 18)

  test("read int using auto handler"):
    val bson = BSONInteger(-285641920)
    val read = summon[BSONReader[Blurs]].readOpt(bson).get
    assertEquals(read.nb, 18)
