package lila.common

class HeapsortTest extends munit.FunSuite:
  import lila.common.Heapsort.{ topN, botN }
  test("empty collection") {
    assertEquals(List.empty[Int].topN(10), List.empty[Int])
  }
  test("select more elements than collection has >> return sorted collection") {
    assertEquals(List.range(0, 10).topN(30), List.range(9, -1, -1))
  }
  test("hand") {
    assertEquals(List.range(0, 10).topN(3), List(9, 8, 7))
    assertEquals(List.range(0, 10).topN(0), List())
    assertEquals(List(5, 3, 1, 4, 2).topN(2), List(5, 4))
    assertEquals(List(5, 3, 1, 4, 2).botN(2), List(1, 2))
  }
  test("Vector") {
    assertEquals(Heapsort.topN(Vector(5, 3, 1, 4, 2), 2)(using Ordering.Int), Vector(5, 4))
    assertEquals(Heapsort.topN(Vector(5, 3, 1, 4, 2), 10)(using Ordering.Int), Vector(5, 4, 3, 2, 1))
  }
