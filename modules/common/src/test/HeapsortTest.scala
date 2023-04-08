package lila.common
import org.specs2.mutable.*

class HeapsortTest extends Specification {
  import lila.common.Heapsort.{ topN, botN }
  "Heapsort" >> {
    "empty collection" >> {
      List.empty[Int].topN(10) === List.empty[Int]
    }
    "select more elements than collection has >> return sorted collection" >> {
      List.range(0, 10).topN(30) === List.range(9, -1, -1)
    }
    "hand" >> {
      List.range(0, 10).topN(3) === List(9, 8, 7)
      List.range(0, 10).topN(0) === List()
      List(5, 3, 1, 4, 2).topN(2) === List(5, 4)
      List(5, 3, 1, 4, 2).botN(2) === List(1, 2)
    }
    "Vector" >> {
      Heapsort.topN(Vector(5, 3, 1, 4, 2), 2)(using Ordering.Int) === Vector(5, 4)
      Heapsort.topN(Vector(5, 3, 1, 4, 2), 10)(using Ordering.Int) === Vector(5, 4, 3, 2, 1)
    }
  }
}
