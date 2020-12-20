package lila.common
import org.specs2.mutable.Specification

class HeapsortTest extends Specification {
  import lila.common.Heapsort.implicits._
  "Heapsort" should {
    "empty collection" in {
      List.empty[Int].topN(10) must_=== List.empty[Int]
    }
    "select more elements than collection has should return sorted collection" in {
      List.range(0, 10).topN(30) must_=== List.range(9, -1, -1)
    }
    "hand" in {
      List.range(0, 10).topN(3) must_=== List(9, 8, 7)
      List.range(0, 10).topN(0) must_=== List()
      List(5, 3, 1, 4, 2).topN(2) must_=== List(5, 4)
      List(5, 3, 1, 4, 2).botN(2) must_=== List(1, 2)
    }
    "Vector" in {
      Heapsort.topN(Vector(5, 3, 1, 4, 2), 2, Ordering.Int) must_=== Vector(5, 4)
      Heapsort.topN(Vector(5, 3, 1, 4, 2), 10, Ordering.Int) must_=== Vector(5, 4, 3, 2, 1)
    }
  }
}
